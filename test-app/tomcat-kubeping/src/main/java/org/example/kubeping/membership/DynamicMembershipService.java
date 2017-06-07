package org.example.kubeping.membership;

import org.apache.catalina.tribes.*;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.util.UUIDGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DynamicMembershipService implements MembershipService, MembershipListener, MessageListener {
    private static final Log log = LogFactory.getLog(DynamicMembershipService.class);

    private Properties properties = new Properties();
    private Channel channel;
    private StaticMember localMember;
    private Membership membership;
    private MembershipListener listener;
    private MessageListener messageListener;

    private MemberProvider memberProvider;
    private RefreshThread refreshThread;

    private byte[] payload;
    private byte[] domain;

    public DynamicMembershipService(MemberProvider memberProvider) {
        this.memberProvider = memberProvider;
        //default values
        properties.setProperty("refreshFrequency", "1000");
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
    }

    @Override
    public void start(int level) throws Exception {
        if ((level & MembershipService.MBR_RX) == 0)
            return;

        // TODO: check that all required properties are set
        // TODO: check that memberProvider is set

        log.info("start(" + level + ")");

        createOrUpdateLocalMember();
        localMember.setMemberAliveTime(100);
        localMember.setPayload(payload);
        localMember.setDomain(domain);
        localMember.setServiceStartTime(System.currentTimeMillis());

        if (membership == null)
            membership = new Membership(localMember);
        else
            membership.reset();

        memberProvider.init(properties);
        fetchMembers(); // Fetch members synchronously once before starting thread

        if (refreshThread == null) {
            refreshThread = new RefreshThread();
            refreshThread.start();
        }
    }

    @Override
    public void stop(int level) {
        log.info("stop(" + level + ")");
        if ((level & MembershipService.MBR_RX) == 0)
            return;

        if (refreshThread != null) {
            refreshThread.running = false;
            refreshThread = null;
        }
    }

    private void fetchMembers() {
        if (memberProvider == null)
            return;

        log.info("fetchMembers()");
        List<? extends Member> members = null;

        try {
            members = memberProvider.getMembers();
        } catch (Exception e) {
            log.info("Exception in memberProvider.getMember(): ", e);
        }

        if (members == null) {
            // TODO: how to handle this?
            log.info("members == null");
            return;
        }

        // Check for new members (i.e. not already in membership)
        for (Member member : members) {
            boolean isNew = true;
            for (Member m : getMembers()) {
                if (Arrays.equals(m.getUniqueId(), member.getUniqueId())) {
                    isNew = false;
                    break;
                }
            }

            if (isNew) {
                log.info("New member: " + member);
                membership.addMember(member);
                memberAdded(member);
            }
        }

        // Check for dead members (i.e. members in membership but not in fetched members)
        for (Member member : getMembers()) {
            boolean isDead = true;
            for (Member m : members) {
                if (Arrays.equals(m.getUniqueId(), member.getUniqueId())) {
                    isDead = false;
                    break;
                }
            }

            if (isDead) {
                log.info("Member is dead: " + member);
                membership.removeMember(member);
                memberDisappeared(member);
            }
        }
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
        log.info("setMessageListener: " + listener);
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
        return true;
    }

    public class RefreshThread extends Thread {
        private boolean running = true;
        private long refreshFrequency;

        RefreshThread() {
            refreshFrequency = Long.parseLong(properties.getProperty("refreshFrequency"));
        }

        @Override
        public void run() {
            while (running) {
                fetchMembers();
                try {
                    Thread.sleep(refreshFrequency);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }
}
