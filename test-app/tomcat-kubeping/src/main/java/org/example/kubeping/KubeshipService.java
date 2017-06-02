package org.example.kubeping;

import org.apache.catalina.tribes.*;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.util.UUIDGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

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

    public static class RefreshThread extends Thread {
        private static final String ENV_PREFIX = "OPENSHIFT_KUBE_PING_";

        private String url;
        private String labels;
        private StreamProvider streamProvider;

        RefreshThread() throws Exception {
            super();
            String namespace = getEnv(ENV_PREFIX + "NAMESPACE");
            if (namespace == null) {
                log.error("Namespace not set; clustering disabled");
                return;
            }

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

                String caCertFile = getEnv(ENV_PREFIX + "CA_CERT_FILE", "KUBERNETES_CA_CERTIFICATE_FILE");
                if (caCertFile == null)
                    caCertFile = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

                // TODO
                //streamProvider = new TokenStreamProvider(saToken, caCertFile);
                streamProvider = new StreamProvider();
            }

            String ver = getEnv(ENV_PREFIX + "API_VERSION");
            if (ver == null)
                ver = "v1";

            url = String.format("%s://%s:%s/api/%s", protocol, host, port, ver);
            labels = getEnv(ENV_PREFIX + "LABELS");
        }

        private static String getEnv(String... keys) {
            String val = null;

            for (String key : keys) {
                val = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getenv(key));
                if (val != null)
                    break;
            }

            return val;
        }

        @Override
        public void run() {
            boolean doRunRefreshThread = true;
            while (doRunRefreshThread) {
                log.info("Refresh pod list");

                String podsUrl = String.format("%s/pods", url);
                JSONObject json = null;
                try {
                    InputStream stream = streamProvider.openStream(url, null, 1000, 1000);
                    json = new JSONObject(new JSONTokener(stream));
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                    return;
                }

                JSONArray items = json.getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject status = item.getJSONObject("status");

                    if (status.getString("phase").equals("Running")) {
                        /* TODO:
                        - get status.podIP
                        - get metadata.uid
                        - transform uid to UniqueId
                        - compare id with existing members
                        - call memberAdded for new member
                        - call memberDisappeared for members not in pod list
                         */

                        String ip = status.getString("podIP");
                        log.info("Pod found: " + ip);
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    private static class StreamProvider {
        public static final TrustManager[] INSECURE_TRUST_MANAGERS = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };
        static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };
        private static final Log log = LogFactory.getLog(StreamProvider.class);
        private final SSLSocketFactory factory;

        public StreamProvider() throws Exception {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, INSECURE_TRUST_MANAGERS, null);
            factory = context.getSocketFactory();
        }

        public InputStream openStream(String url, Map<String, String> headers, int connectTimeout, int readTimeout) throws IOException {
            URLConnection connection = openConnection(url, headers, connectTimeout, readTimeout);
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = HttpsURLConnection.class.cast(connection);
                httpsConnection.setHostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
                httpsConnection.setSSLSocketFactory(factory);
                log.debug(String.format("Using HttpsURLConnection with SSLSocketFactory [%s] for url [%s].", factory, url));
            } else {
                log.debug(String.format("Using URLConnection for url [%s].", url));
            }

            return connection.getInputStream();
        }

        public URLConnection openConnection(String url, Map<String, String> headers, int connectTimeout, int readTimeout) throws IOException {
            log.debug(String.format("%s opening connection: url [%s], headers [%s], connectTimeout [%s], readTimeout [%s]", getClass().getSimpleName(), url, headers, connectTimeout, readTimeout));
            URLConnection connection = new URL(url).openConnection();
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (connectTimeout < 0 || readTimeout < 0) {
                throw new IllegalArgumentException(
                        String.format("Neither connectTimeout [%s] nor readTimeout [%s] can be less than 0 for URLConnection.", connectTimeout, readTimeout));
            }
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            return connection;
        }
    }
}
