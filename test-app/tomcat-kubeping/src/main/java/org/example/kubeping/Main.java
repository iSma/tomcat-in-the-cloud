package org.example.kubeping;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.StaticMember;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws LifecycleException, IOException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        File base = new File(System.getProperty("java.io.tmpdir"));
        Context ctx = tomcat.addContext("", base.getAbsolutePath());
        Tomcat.addServlet(ctx, "TestServlet", new TestServlet());
        ctx.addServletMappingDecoded("/", "TestServlet");

        SimpleTcpCluster cluster = new SimpleTcpCluster();
        tomcat.getEngine().setCluster(cluster);

        ctx.setName("{CTX}");
        ctx.setDistributable(true);

        // Start Replication receiver and transmitter
        // Don't start Membership receiver and transmitter (MBR_{RX,TX}_SEQ)
        cluster.setChannelStartOptions(Channel.SND_RX_SEQ | Channel.SND_TX_SEQ);

        GroupChannel channel = (GroupChannel) cluster.getChannel();

        channel.addInterceptor(new TcpPingInterceptor());
        channel.addInterceptor(new TcpFailureDetector());
        channel.addInterceptor(new MessageDispatchInterceptor());

        StaticMembershipInterceptor membership = new StaticMembershipInterceptor();
        membership.addStaticMember(member(2));
        membership.addStaticMember(member(3));
        membership.addStaticMember(member(4));

        channel.addInterceptor(membership);

        tomcat.start();
        tomcat.getServer().await();
    }

    private static Member member(int i) {
        StaticMember member = new StaticMember();
        member.setHost("172.17.0." + i);
        member.setPort(4000);
        // Create dummy id = {0, 0, ..., 0, i}
        byte[] id = new byte[16];
        id[15] = (byte) i;
        member.setUniqueId(id);
        return member;
    }
}
