package org.example.tomcat.test;

import javax.servlet.http.HttpSession;

public class SessionInfo {
    private final String id;
    private final boolean isNew;
    private final String server;
    private int counter;

    public SessionInfo(HttpSession session, String serverInfo) {
        this.server = serverInfo;
        this.id = session.getId();
        this.isNew = session.isNew();
        this.counter = (int) session.getAttribute("counter");
    }

    public String getId() {
        return id;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getServer() {
        return server;
    }

    public int getCounter() {
        return counter;
    }
}
