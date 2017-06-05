package org.example.kubeping;

import org.apache.catalina.tribes.*;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.util.UUIDGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.example.kubeping.stream.StreamProvider;
import org.example.kubeping.stream.TokenStreamProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

public class KubeshipService implements MembershipService, MembershipListener, MessageListener {
    private static final Log log = LogFactory.getLog(KubeshipService.class);

    private Properties properties = new Properties();
    private Channel channel;
    private StaticMember localMember;
    private Membership membership;
    private MembershipListener listener;
    private MessageListener messageListener;

    private RefreshThread refreshThread;

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
        // TODO: check level (only start for MBR_TX or MBR_TX), don't start twice

        log.info("start(" + level + ")");

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

        if (refreshThread == null) {
            refreshThread = new RefreshThread();
            refreshThread.start();
        }
    }

    @Override
    public void stop(int level) {
        // TODO
        log.info("STOP + " + level);
    }

    @Override
    public boolean hasMembers() {
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

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void removeMessageListener() {
        this.messageListener = null;
    }

    @Override
    public void setPayload(byte[] payload) {
        // TODO: what does this method do?
        log.info("setPayload: " + Arrays.toString(payload));
        this.payload = payload;
        if (localMember != null) {
            localMember.setPayload(payload);
            // impl.send(false)
        }
    }

    @Override
    public void setDomain(byte[] domain) {
        // TODO: what does this method do?
        log.info("setDomain: " + Arrays.toString(domain));
        this.domain = domain;
        if (localMember != null) {
            localMember.setDomain(domain);
            // impl.send(false)
        }
    }

    @Override
    public void broadcast(ChannelMessage message) throws ChannelException {
        // TODO: what does this method do?
        log.info("broadcast: " + message);
    }

    @Override
    public Channel getChannel() {
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

    @Override
    public void messageReceived(ChannelMessage msg) {
        log.info("messageReceived: " + msg);
        if (messageListener != null && messageListener.accept(msg))
            messageListener.messageReceived(msg);
    }

    @Override
    public boolean accept(ChannelMessage msg) {
        return false;
    }

    public class RefreshThread extends Thread {
        private static final String ENV_PREFIX = "OPENSHIFT_KUBE_PING_";

        private String url;
        private StreamProvider streamProvider;
        private boolean running;

        RefreshThread() {
            super();
            try {
                init();
                running = true;
            } catch (IOException e) {
                e.printStackTrace();
                running = false;
            }
        }

        private void init() throws IOException {
            String namespace = getEnv(ENV_PREFIX + "NAMESPACE");
            if (namespace == null || namespace.length() == 0)
                throw new RuntimeException("Namespace not set; clustering disabled");

            log.info(String.format("Namespace [%s] set; clustering enabled", namespace));

            String protocol = getEnv(ENV_PREFIX + "MASTER_PROTOCOL");
            String host;
            String port;

            String certFile = getEnv(ENV_PREFIX + "CLIENT_CERT_FILE", "KUBERNETES_CLIENT_CERTIFICATE_FILE");

            if (certFile != null) {
                if (protocol == null)
                    protocol = "http";

                host = getEnv(ENV_PREFIX + "MASTER_HOST", "KUBERNETES_RO_SERVICE_HOST");
                port = getEnv(ENV_PREFIX + "MASTER_PORT", "KUBERNETES_RO_SERVICE_PORT");

                String keyFile = getEnv(ENV_PREFIX + "CLIENT_KEY_FILE", "KUBERNETES_CLIENT_KEY_FILE");
                String keyPassword = getEnv(ENV_PREFIX + "CLIENT_KEY_PASSWORD", "KUBERNETES_CLIENT_KEY_PASSWORD");

                String keyAlgo = getEnv(ENV_PREFIX + "CLIENT_KEY_ALGO", "KUBERNETES_CLIENT_KEY_ALGO");
                if (keyAlgo == null)
                    keyAlgo = "RSA";

                String caCertFile = getEnv(ENV_PREFIX + "CA_CERT_FILE", "KUBERNETES_CA_CERTIFICATE_FILE");
                if (caCertFile == null)
                    caCertFile = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

                // TODO
                // streamProvider = new CertificateStreamProvider(certFile, keyFile, keyPassword, keyAlgo, caCertFile);
            } else {
                if (protocol == null)
                    protocol = "https";

                host = getEnv(ENV_PREFIX + "MASTER_HOST", "KUBERNETES_SERVICE_HOST");
                port = getEnv(ENV_PREFIX + "MASTER_PORT", "KUBERNETES_SERVICE_PORT");
                String saTokenFile = getEnv(ENV_PREFIX + "SA_TOKEN_FILE");
                if (saTokenFile == null)
                    saTokenFile = "/var/run/secrets/kubernetes.io/serviceaccount/token";

                byte[] bytes = new byte[0];
                bytes = Files.readAllBytes(FileSystems.getDefault().getPath(saTokenFile));

                String saToken = new String(bytes);

                String caCertFile = getEnv(ENV_PREFIX + "CA_CERT_FILE", "KUBERNETES_CA_CERTIFICATE_FILE");
                if (caCertFile == null)
                    caCertFile = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

                streamProvider = new TokenStreamProvider(saToken, caCertFile);
            }

            String ver = getEnv(ENV_PREFIX + "API_VERSION");
            if (ver == null)
                ver = "v1";

            String labels = getEnv(ENV_PREFIX + "LABELS");

            namespace = URLEncoder.encode(namespace, "UTF-8");
            labels = labels == null ? null : URLEncoder.encode(labels, "UTF-8");

            url = String.format("%s://%s:%s/api/%s/namespaces/%s/pods", protocol, host, port, ver, namespace);

            if (labels != null && labels.length() > 0)
                url = url + "?labelSelector=" + labels;
        }

        private String getEnv(String... keys) {
            String val = null;

            for (String key : keys) {
                val = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getenv(key));
                if (val != null)
                    break;
            }

            return val;
        }

        private byte[] idToBytes(String id) {
            if (id == null)
                return null;

            id = id.replaceAll("-", "");
            if (id.length() < 32)
                return null;

            byte[] bytes = new byte[16];
            for (int i = 0; i < bytes.length; i++)
                bytes[i] = (byte) Integer.parseInt(id.substring(i * 2, i * 2 + 2), 16);

            return bytes;
        }

        @Override
        public void run() {

            while (running) {
                fetchPeers();

                try {
                    // TODO: extract sleep time to KubeshipService.properties
                    Thread.sleep(10000);
                } catch (InterruptedException ignore) {
                }
            }
        }

        private void fetchPeers() {
            log.info("Refresh pod list");
            Map<String, String> headers = new HashMap<>();

            JSONObject json = null;
            try {
                // TODO: extract timeout values to KubeshipService.properties
                InputStream stream = streamProvider.openStream(url, headers, 1000, 1000);
                json = new JSONObject(new JSONTokener(stream));
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
                return;
            }

            JSONArray items = json.getJSONArray("items");
            List<MemberImpl> foundMembers = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject status = item.getJSONObject("status");
                JSONObject metadata = item.getJSONObject("metadata");

                if (!status.getString("phase").equals("Running"))
                    continue;

                // TODO: how to handle current pod?

                String ip = status.getString("podIP");
                String uid = metadata.getString("uid");
                byte[] id = idToBytes(uid);
                if (id == null) {
                    log.info("UID " + uid + " malformed; ignoring this pod");
                    continue;
                }

                MemberImpl member = null;
                try {
                    // TODO: read port from properties
                    member = new MemberImpl(ip, 4000, 0);
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                    continue;
                }

                member.setUniqueId(id);
                foundMembers.add(member);

                // Check if member is already known (i.e. already in membership)
                boolean isNew = true;
                for (Member m : getMembers()) {
                    if (Arrays.equals(m.getUniqueId(), member.getUniqueId())) {
                        isNew = false;
                        break;
                    }
                }

                if (isNew) {
                    log.info("New: " + member);
                    membership.addMember(member);
                    memberAdded(member);
                }
            }

            // Check for dead members (i.e. members in membership but not in fetched members)
            for (Member member : getMembers()) {
                boolean isDead = true;
                for (Member m : foundMembers) {
                    if (Arrays.equals(m.getUniqueId(), member.getUniqueId())) {
                        isDead = false;
                        break;
                    }
                }

                if (isDead) {
                    log.info("Dead: " + member);
                    membership.removeMember(member);
                    memberDisappeared(member);
                }
            }
        }
    }
}
