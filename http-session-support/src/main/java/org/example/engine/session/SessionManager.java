package org.example.engine.session;

import jakarta.servlet.http.HttpSession;
import org.example.engine.ServletContextImpl;
import org.example.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements Runnable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    // 引用ServletContext:
    ServletContextImpl servletContext;
    // 持有SessionID -> Session:
    Map<String, HttpSessionImpl> sessionMap = new ConcurrentHashMap<>();
    // Session默认过期时间(秒):
    int inactiveInterval;

    public SessionManager(ServletContextImpl servletContext, int interval) {
        this.servletContext = servletContext;
        this.inactiveInterval = interval;
        Thread t = new Thread(this, "Session-Cleanup-Thread");
        t.setDaemon(true);
        t.start();
    }

    public HttpSession getSession(String sessionId) {
        HttpSessionImpl session = sessionMap.get(sessionId);
        if (session == null) {
            session = new HttpSessionImpl(this.servletContext, sessionId, inactiveInterval);
            sessionMap.put(sessionId, session);
        } else {
            session.lastAccessedTime = System.currentTimeMillis();
        }
        return session;
    }

    public void remove(HttpSession session) {
        this.sessionMap.remove(session.getId());
    }

    public int getInactiveInterval() {
        return inactiveInterval;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                Thread.sleep(60_000L);
            } catch (InterruptedException e) {
                break;
            }
            long now = System.currentTimeMillis();
            for (String sessionId : sessionMap.keySet()) {
                HttpSession session = sessionMap.get(sessionId);
                if (session.getLastAccessedTime() + session.getMaxInactiveInterval() * 1000L < now) {
                    logger.warn("remove expired session: {}, last access time: {}", sessionId, DateUtils.formatDateTimeGMT(session.getLastAccessedTime()));
                    session.invalidate();
                }
            }
        }
    }
}
