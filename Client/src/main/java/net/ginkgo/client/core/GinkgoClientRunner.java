package net.ginkgo.client.core;

import net.ginkgo.client.annotation.ClientApplication;
import net.ginkgo.client.entity.Packet;
import net.ginkgo.client.entity.PacketError;
import net.ginkgo.client.entity.PacketHandshake;
import net.ginkgo.client.security.AbstractPacketEncryption;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.BufferUnpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

public class GinkgoClientRunner {

    private final static ExecutorService PACKET_HANDLE = new ThreadPoolExecutor(5, 10,
            10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));

    static Selector selector;
    static SocketChannel socket;

    /**
     * 客户端网络主线程
     * <p>
     *
     * @param application 客户端应用程序
     */
    public static void runClient(ClientApplication application){
        System.out.println("Connecting to server...");
        try (SocketChannel socket = SocketChannel.open()){
            GinkgoClientRunner.socket = socket;
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(application.ip(), application.port()));
            selector = Selector.open();
            socket.register(selector, SelectionKey.OP_CONNECT);
            while (true) {
                if (socket.isOpen()) {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isConnectable()) {
                            while (!socket.finishConnect()) {
                                System.out.println("Wait for connect...");
                            }
                            socket.write(ByteBuffer.wrap(new byte[]{0}));  //发送握手申请（需要获取服务器相关信息）
                            socket.register(selector, SelectionKey.OP_READ);
                            System.out.println("Server connection has been established!");
                        }

                        if (key.isWritable()) {
                            socket.write((ByteBuffer) key.attachment());
                            socket.register(selector, SelectionKey.OP_READ);
                        }

                        if (key.isReadable()) {
                            try {
                                ByteBuffer buffer = ByteBuffer.allocate(4);
                                byte[] bytes = new byte[0];
                                int len, pos = 0;
                                while ((len = socket.read(buffer)) > 0){
                                    bytes = Arrays.copyOf(bytes, bytes.length + len);
                                    buffer.flip();
                                    while (buffer.hasRemaining()){
                                        bytes[pos++] = buffer.get();
                                    }
                                    buffer.clear();
                                }
                                if(bytes.length == 0) break;  //服务器关闭
                                bytes = unZip(bytes);
                                if(bytes == null) throw new IllegalStateException("Error server packet!");

                                AbstractPacketEncryption encryption = GinkgoRegistry.encryption;
                                if(encryption != null) bytes = encryption.decode(bytes);

                                if(bytes.length > 0) {
                                    MessagePack pack = new MessagePack();
                                    BufferUnpacker unpacker = pack.createBufferUnpacker(bytes);
                                    String type = unpacker.readString();
                                    Class<? extends Packet> clazz = GinkgoRegistry.convertPacket(type);
                                    Packet packet = clazz.newInstance();
                                    packet.read(unpacker);
                                    if(packet instanceof PacketHandshake){
                                        GinkgoRegistry.setEncryption((PacketHandshake) packet);
                                        GinkgoNetwork.setSessionID(((PacketHandshake) packet).getSessionID());
                                    }else if(packet instanceof PacketError){
                                        throw new IllegalStateException("Server has returned an error code: "+ ((PacketError) packet).getCode());
                                    }else {
                                        PACKET_HANDLE.submit(() -> GinkgoNetwork.receivePacket(packet));
                                    }
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                key.cancel();
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
        return b;
    }
}
