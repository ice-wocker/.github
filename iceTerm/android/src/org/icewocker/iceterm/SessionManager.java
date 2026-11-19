package org.icewocker.iceterm;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    public static class TabSession {
        public final int id;
        public String title;
        public final TerminalBuffer buffer;
        public final ShellSession shell;
        public boolean isAlive = true;

        public TabSession(int id, String title, ShellSession.OutputListener listener) {
            this.id = id;
            this.title = title;
            this.buffer = new TerminalBuffer(2000);
            this.shell = new ShellSession(listener);
        }
    }

    public interface SessionChangeListener {
        void onTabSwitched(TabSession active);
        void onTabAdded(TabSession tab);
        void onTabRemoved(int tabId);
    }

    private final List<TabSession> sessions = new ArrayList<TabSession>();
    private int activeIndex = 0;
    private int nextId = 1;
    private SessionChangeListener listener;

    public SessionManager(SessionChangeListener listener) {
        this.listener = listener;
    }

    public synchronized TabSession createSession(String title, ShellSession.OutputListener outputListener) {
        TabSession session = new TabSession(nextId++, title, outputListener);
        sessions.add(session);
        activeIndex = sessions.size() - 1;
        session.shell.start();

        if (listener != null) {
            listener.onTabAdded(session);
            listener.onTabSwitched(session);
        }
        return session;
    }

    public synchronized TabSession getActiveSession() {
        if (activeIndex >= 0 && activeIndex < sessions.size()) {
            return sessions.get(activeIndex);
        }
        return null;
    }

    public synchronized void switchToTab(int index) {
        if (index >= 0 && index < sessions.size() && index != activeIndex) {
            activeIndex = index;
            if (listener != null) {
                listener.onTabSwitched(sessions.get(activeIndex));
            }
        }
    }

    public synchronized void closeSession(int index) {
        if (index >= 0 && index < sessions.size()) {
            TabSession s = sessions.remove(index);
            s.shell.destroy();
            s.isAlive = false;

            if (listener != null) {
                listener.onTabRemoved(s.id);
            }

            if (sessions.isEmpty()) {
                activeIndex = -1;
            } else {
                if (activeIndex >= sessions.size()) {
                    activeIndex = sessions.size() - 1;
                }
                if (listener != null) {
                    listener.onTabSwitched(sessions.get(activeIndex));
                }
            }
        }
    }

    public synchronized List<TabSession> getSessions() {
        return new ArrayList<TabSession>(sessions);
    }

    public synchronized int getActiveIndex() {
        return activeIndex;
    }

    public synchronized void destroyAll() {
        for (TabSession s : sessions) {
            s.shell.destroy();
        }
        sessions.clear();
    }
}
