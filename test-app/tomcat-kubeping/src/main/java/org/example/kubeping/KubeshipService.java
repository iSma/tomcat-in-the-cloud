package org.example.kubeping;

import org.apache.catalina.tribes.*;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.util.UUIDGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Properties;

public class KubeshipService implements MembershipService, MembershipListener {
    private static final Log log = LogFactory.getLog(McastService.class);

    private Properties properties = new Properties();
    private Channel channel;
    private StaticMember localMember;
    private Membership membership;
    private MembershipListener listener;

    private byte[] payload;
    private byte[] domain;

    KubeshipService() {
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void start() throws Exception {
        log.info("START");
        start(MembershipService.MBR_RX);
        start(MembershipService.MBR_TX);

    }

    @Override
    public void start(int level) throws Exception {
        // TODO

        log.info("start(" + level + ")");

//        if ( impl != null ) {
//            impl.start(level);
//            return;
//        }

        String host = properties.getProperty("tcpListenHost");
        int port = Integer.parseInt(properties.getProperty("tcpListenPort"));
        int securePort = Integer.parseInt(properties.getProperty("tcpSecurePort"));
        int udpPort = Integer.parseInt(properties.getProperty("udpListenPort"));

        createOrUpdateLocalMember();
        localMember.setMemberAliveTime(100);
        localMember.setPayload(payload);
        localMember.setDomain(domain);
        localMember.setServiceStartTime(System.currentTimeMillis());

        if (membership == null)
            membership = new Membership(localMember);
        else
            membership.reset();

        //impl.start(level);
    }

    @Override
    public void stop(int level) {
        // TODO
        log.info("STOP + " + level);
    }

    @Override
    public boolean hasMembers() {
        log.info("hasMembers()");
        return membership != null && membership.hasMembers();
    }

    @Override
    public Member getMember(Member mbr) {
        log.info("getMember: " + mbr);
        if (membership == null)
            return null;
        return membership.getMember(mbr);
    }

    @Override
    public Member[] getMembers() {
        log.info("getMembers()");
        if (membership == null)
            return new Member[0];
        return membership.getMembers();
    }

    @Override
    public Member getLocalMember(boolean incAliveTime) {
        log.info("getLocalMember: " + incAliveTime);
        if (incAliveTime && localMember != null)
            localMember.setMemberAliveTime(System.currentTimeMillis() - localMember.getServiceStartTime());
        return localMember;
    }

    @Override
    public String[] getMembersByName() {
        // TODO
        log.info("getMembersByName()");
        return new String[0];
    }

    @Override
    public Member findMemberByName(String name) {
        // TODO
        log.info("findMemberByName: " + name);
        return null;
    }

    @Override
    public void setLocalMemberProperties(String listenHost, int listenPort, int securePort, int udpPort) {
        log.info(String.format("setLocalMemberProperties(%s, %d, %d, %d)", listenHost, listenPort, securePort, udpPort));
        properties.setProperty("tcpListenHost", listenHost);
        properties.setProperty("tcpListenPort", String.valueOf(listenPort));
        properties.setProperty("udpListenPort", String.valueOf(udpPort));
        properties.setProperty("tcpSecurePort", String.valueOf(securePort));

        try {
            createOrUpdateLocalMember();

            localMember.setPayload(this.payload);
            localMember.setDomain(this.domain);
            localMember.getData(true, true);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void createOrUpdateLocalMember() throws IOException {
        String host = properties.getProperty("tcpListenHost");
        int port = Integer.parseInt(properties.getProperty("tcpListenPort"));
        int securePort = Integer.parseInt(properties.getProperty("tcpSecurePort"));
        int udpPort = Integer.parseInt(properties.getProperty("udpListenPort"));

        if (localMember == null) {
            localMember = new StaticMember(host, port, 0);
            localMember.setUniqueId(UUIDGenerator.randomUUID(true));
            localMember.setLocal(true);
        } else {
            localMember.setHostname(host);
            localMember.setPort(port);
        }

        localMember.setSecurePort(securePort);
        localMember.setUdpPort(udpPort);
        localMember.getData(true, true);
    }

    @Override
    public void setMembershipListener(MembershipListener listener) {
        this.listener = listener;
        log.info("setMembershipListener: " + listener);
    }

    @Override
    public void removeMembershipListener() {
        this.listener = null;
        log.info("removeMembershipListener");
    }

    @Override
    public void setPayload(byte[] payload) {
        log.info("setPayload: " + Arrays.toString(payload));
        this.payload = payload;
        if (localMember != null) {
            localMember.setPayload(payload);
            // impl.send(false)
        }
    }

    @Override
    public void setDomain(byte[] domain) {
        log.info("setDomain: " + Arrays.toString(domain));
        this.domain = domain;
        if (localMember != null) {
            localMember.setDomain(domain);
            // impl.send(false)
        }
    }

    @Override
    public void broadcast(ChannelMessage message) throws ChannelException {
        // TODO
        log.info("broadcast: " + message);
    }

    @Override
    public Channel getChannel() {
        log.info("getChannel()");
        return this.channel;
    }

    @Override
    public void setChannel(Channel channel) {
        log.info("setChannel: " + channel);
        this.channel = channel;
    }

    @Override
    public void memberAdded(Member member) {
        log.info("memberAdded: " + member);
        if (listener != null)
            listener.memberAdded(member);
    }

    @Override
    public void memberDisappeared(Member member) {
        log.info("memberDisappeared: " + member);
        if (listener != null)
            listener.memberDisappeared(member);
    }

    public static class RefreshThread extends Thread {
        private static final String ENV_PREFIX = "OPENSHIFT_KUBE_PING_";

        RefreshThread() {
            super();
            String protocol = getEnv("MASTER_PROTOCOL", "https");
            String host = getEnv("MASTER_HOST");
            String port = getEnv("MASTER_PORT");

            String ver = getEnv("API_VERSION", "v1");
            String url = String.format("%s://%s:%s/api/%s", protocol, host, port, ver);
        }

        private static String getEnv(String suffix, String def) {
            String key = ENV_PREFIX + suffix;
            String val = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getenv(key));
            return val == null ? def : val;
        }

        private static String getEnv(String suffix) {
            return getEnv(suffix, null);
        }

        public static JSONObject request(String url) throws IOException {
            JSONObject json = null;
            HttpURLConnection conn = null;

            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("HTTP Request Failed: " + conn.getResponseCode());
                }

                json = new JSONObject(new JSONTokener(conn.getInputStream()));
            } finally {
                if (conn != null)
                    conn.disconnect();
            }

            return json;
        }

        @Override
        public void run() {
            boolean doRunRefreshThread = true;
            while (doRunRefreshThread) {
                /*
                TODO
                - construct URL
                - make request
                - get desired info from JSON
                 */
            }
        }
    }
}
