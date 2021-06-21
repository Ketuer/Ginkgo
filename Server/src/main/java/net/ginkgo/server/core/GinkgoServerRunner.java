package net.ginkgo.server.core;

import net.ginkgo.server.annotation.ServerApplication;
import net.ginkgo.server.command.AbstractCommand;
import net.ginkgo.server.command.Confirmable;
import net.ginkgo.server.entity.PacketError;
import net.ginkgo.server.entity.PacketHandshake;
import net.ginkgo.server.entity.Session;
import net.ginkgo.server.security.AbstractPacketEncryption;
import net.ginkgo.server.service.AbstractPacketService;
import net.ginkgo.server.entity.Packet;
import net.ginkgo.server.exception.BadPacketException;
import net.ginkgo.server.logger.ILogger;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class GinkgoServerRunner {
    private final static ExecutorService SERVICE_DISPATCHER = new ThreadPoolExecutor(15, 30,
            10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(30));
    private final static Thread CONSOLE_COMMAND = new Thread(GinkgoServerRunner::runConsole, "ConsoleCommand");
    private final static Thread SCHEDULE_CENTER = new Thread(GinkgoTaskCenter::startTaskCenter, "ScheduleCenter");
    private final static Thread SESSION_MANAGER = new Thread(GinkgoSessionManager::runSessionManager, "SessionManager");
    private static Selector SELECTOR;
    private static boolean endSignal = false;

    private static final ILogger logger = GinkgoRegistry.getLogger(GinkgoServerRunner.class);

    /**
     * 运行服务器主线程
     * @param application 服务器应用程序
     */
    public static void runServer(ServerApplication application){
        Thread.currentThread().setName(application.value());

        try (ServerSocketChannel server = ServerSocketChannel.open()){
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(application.ip(), application.port()));
            logger.info("The server is running on port "+application.port()+" !");
            logger.info("Creating console command and task center process...");
            CONSOLE_COMMAND.start();
            SCHEDULE_CENTER.start();
            SESSION_MANAGER.start();

            Selector selector = Selector.open();
            SELECTOR = selector;
            server.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server startup is complete!");

            while (true) {
                selector.select();
                if(endSignal) break; //StopServer

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        SocketChannel socket = server.accept();
                        socket.configureBlocking(false);
                        socket.register(selector, SelectionKey.OP_READ);
                    }
                    int size = 0;
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        try {
                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            byte[] bytes = new byte[0];
                            int len, pos = 0;
                            while ((len = channel.read(buffer)) > 0){
                                bytes = Arrays.copyOf(bytes, bytes.length + len);
                                buffer.flip();
                                while (buffer.hasRemaining()){
                                    bytes[pos++] = buffer.get();
                                }
                                buffer.clear();
                            }
                            size = bytes.length;
                            if(size <= 0) {  //Fix epoll bug
                                key.cancel();
                                channel.close();
                            }
                            if(size > 0) receivePacket(channel, bytes);
                        }catch (Exception e){
                            logger.warn("A bad packet (size: "+size+") form "+channel.getRemoteAddress().toString()
                                    +", and the channel has been closed!");
                            key.cancel();
                            channel.close();
                        }
                    }
                }
            }

            SCHEDULE_CENTER.interrupt();
            SCHEDULE_CENTER.join();

            List<Runnable> list = SERVICE_DISPATCHER.shutdownNow();
            while (!list.isEmpty()){
                logger.warn("Now there is still "+list.size()+" thread(s) running, the system has sent an interrupt notification, " +
                        "and the thread status will be checked again in 3 seconds...");
                TimeUnit.SECONDS.sleep(3);
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e);
        }
        logger.info("Server is shutdown!");
        System.exit(0);
    }

    /**
     * 发送数据包
     * @param channel 通道
     * @param packet 数据包
     */
    private static void sendPacket(SocketChannel channel, Packet packet){
        try {
            MessagePack pack = new MessagePack();
            BufferPacker packer = pack.createBufferPacker();
            packer.write(packet.getClass().getSimpleName());
            packet.write(packer);
            byte[] bytes = packer.toByteArray();

            if(!(packet instanceof PacketHandshake)){  //Except handshake
                AbstractPacketEncryption encryption = GinkgoRegistry.getEncryption();
                bytes = encryption.encode(bytes);  //Encryption encode
            }

            bytes = zip(bytes);   //Zip compress
            if(bytes == null) throw new IOException("Can't zip!");
            channel.write(ByteBuffer.wrap(bytes));
        }catch (IOException e){
            logger.error(e);
        }
    }

    /**
     * 接收数据包（仅进行预处理）
     * @param channel 通道
     * @param bytes 数据
     * @throws BadPacketException 无法解析数据包
     */
    private static void receivePacket(SocketChannel channel, byte[] bytes) throws BadPacketException{
        AbstractPacketEncryption encryption = GinkgoRegistry.getEncryption();
        try {
            if(bytes.length == 1 && bytes[0] == 0) {
                sendPacket(channel, encryption.handshake()); //发送服务器握手数据
                logger.info(channel.getRemoteAddress()+" has connected!");
                return;
            }

            bytes = unZip(bytes);
            if(bytes == null) return;
            bytes = encryption.decode(bytes);  //进行数据解密

            MessagePack pack = new MessagePack();
            BufferUnpacker unpacker = pack.createBufferUnpacker(bytes);
            String type = unpacker.readString();
            Class<? extends Packet> clazz = GinkgoRegistry.convertPacket(type);
            if(clazz == null) throw new BadPacketException("Received unknown packet!");

            Packet packet = clazz.newInstance();
            Session session = GinkgoSessionManager.activeSession(unpacker.readString());
            if(session == null) {
                sendPacket(channel, new PacketError(501)); // 501错误: Session过期或不存在
                return;
            }
            packet.setSession(session);
            packet.read(unpacker);
            InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
            for (AbstractPacketService service : GinkgoRegistry.dispatchPacket(clazz)) {
                SERVICE_DISPATCHER.submit(() -> {
                    try {
                        if(channel.isOpen() && service.filter(address, packet)){
                            sendPacket(channel, service.receivePacket(packet));
                        }else {
                            channel.close();
                        }
                    }catch (Exception e){
                        ILogger innerLogger = GinkgoRegistry.getLogger(service.getClass());
                        innerLogger.error(e);
                    }
                });
            }
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            logger.error(e);
        }
    }

    /**
     * Zip压缩数据
     * @param data 数据
     * @return 压缩数据
     */
    public static byte[] zip(byte[] data){
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bos)){
            ZipEntry entry = new ZipEntry("zip");
            entry.setSize(data.length);
            zip.putNextEntry(entry);
            zip.write(data);
            zip.closeEntry();
            return bos.toByteArray();
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * Zip解压缩
     * @param data 数据
     * @return 解压数据
     */
    public static byte[] unZip(byte[] data) {
        byte[] b = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ZipInputStream zip = new ZipInputStream(bis)){
            while (zip.getNextEntry() != null) {
                byte[] buf = new byte[1024];
                int num;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((num = zip.read(buf, 0, buf.length)) != -1) {
                    baos.write(buf, 0, num);
                }
                b = baos.toByteArray();
                baos.flush();
                baos.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
        return b;
    }

    public static void stopServer(){
        logger.info("Stopping server...");
        endSignal = true;
        SELECTOR.wakeup();
    }

    private static void runConsole(){
        Scanner scanner = new Scanner(System.in);
        logger.info("Console command process is running!");
        while (true){
            String raw = scanner.nextLine();
            String[] input = raw.split(" ");

            String arg = null;
            List<String> list = new ArrayList<>();
            Map<String, String[]> map = new HashMap<>();
            for (String s : Arrays.copyOfRange(input, 1, input.length)) {
                if(arg == null){
                    if(s.startsWith("-")){
                       arg = s.substring(1);
                    }else {
                        logger.warn("Wrong command format!");
                    }
                }else {
                    if(s.startsWith("-")){
                        map.put(arg, list.toArray(new String[0]));
                        list.clear();
                        arg = s.substring(1);
                    }else {
                        list.add(s);
                    }
                }
            }
            map.put(arg, list.toArray(new String[0]));

            AbstractCommand command = GinkgoRegistry.matchCommand(input[0].toLowerCase());
            if(command == null) continue;
            if(command instanceof Confirmable){
                Confirmable confirmable = (Confirmable) command;
                System.err.println(confirmable.message());
                if(!confirmable.confirm(scanner.nextLine())){
                    System.out.println("command canceled!");
                    continue;
                }
            }
            logger.silent("Console has do command: "+raw);
            try{
                command.doCommand(map);
            }catch (Exception e){
                logger.warn("An error occurred while executing the command!");
                logger.error(e);
            }
        }
    }
}
