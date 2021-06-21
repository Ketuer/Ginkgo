package net.ginkgo.server.core;

import net.ginkgo.server.entity.Session;
import net.ginkgo.server.logger.ILogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GinkgoSessionManager {
    private static final DelayQueue<SessionDelayed> DELAYED_QUEUE = new DelayQueue<>();
    private static final Map<String, Session> SESSIONS = new HashMap<>();

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    public static void runSessionManager(){
        ILogger logger = GinkgoRegistry.getLogger(GinkgoSessionManager.class);
        while (true) {
            try{
                SessionDelayed delayed = DELAYED_QUEUE.take();
                writeLock.lock();    //挂写锁，防止同时刷新Session生存期
                SESSIONS.remove(delayed.sessionID);
                writeLock.unlock();
                System.out.println("已移除："+delayed.sessionID);
            } catch (InterruptedException e) {
                break;
            }
        }
        logger.info("Session manager has shutdown!");
    }

    /**
     * 每次调用都会刷新Session过期时间，以维持当前会话。
     * @param sessionID 会话ID
     * @return 若没找到此Session，则返回null
     */
    public static Session activeSession(String sessionID){
        synchronized (sessionID.intern()){    //多线程刷新同一个ID的session生存时间
            readLock.lock();    //挂读锁，等Session清理线程结束（危险期），随便多少线程获取都OK
            Session session = SESSIONS.get(sessionID);
            if(session == null) {
                readLock.unlock();
                return null;
            }
            SessionDelayed delayed = new SessionDelayed(session.getID(),
                    GinkgoConfiguration.networkConfig.getSessionExpiredTime());
            DELAYED_QUEUE.remove(delayed);
            DELAYED_QUEUE.put(delayed);
            readLock.unlock();
            return session;
        }
    }

    /**
     * 新建一个Session会话，会被保存在服务端直到过期。
     * @return 会话
     */
    public static Session createNewSession(){
        Session session = new ServerSession(UUID.randomUUID().toString());
        DELAYED_QUEUE.put(new SessionDelayed(session.getID(),
                GinkgoConfiguration.networkConfig.getSessionExpiredTime()));
        SESSIONS.put(session.getID(), session);
        return session;
    }

    private static class SessionDelayed implements Delayed{
        private final long removeTime;
        private final String sessionID;

        public SessionDelayed(String sessionID, long liveTime){
            this.sessionID = sessionID;
            this.removeTime = TimeUnit.NANOSECONDS.convert(liveTime, TimeUnit.SECONDS) + System.nanoTime();
        }

        public String getSessionID() {
            return sessionID;
        }

        public long getRemoveTime() {
            return removeTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(removeTime - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (o == null) return 1;
            if (o == this) return  0;
            if (o instanceof SessionDelayed){
                SessionDelayed sessionDelayed = (SessionDelayed)o;
                return Long.compare(removeTime, sessionDelayed.getRemoveTime());
            }
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SessionDelayed) {
                if (obj==this) return true;
                return ((SessionDelayed) obj).getSessionID().equals(this.getSessionID());
            }
            return false;
        }
    }

    private static class ServerSession implements Session{

        String id;
        Map<String, Object> attrs = new ConcurrentHashMap<>();

        public ServerSession(String id){
            this.id = id;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public void setAttribute(String key, Object object) {
            attrs.put(key, object);
        }

        @Override
        public Object getAttribute(String key) {
            return attrs.get(key);
        }

        @Override
        public boolean hasAttribute(String key) {
            return attrs.containsKey(key);
        }
    }
}
