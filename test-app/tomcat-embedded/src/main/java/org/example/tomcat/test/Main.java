package org.example.tomcat.test;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.ha.session.DeltaManager;
import org.apache.catalina.ha.session.JvmRouteBinderValve;
import org.apache.catalina.ha.tcp.ReplicationValve;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.nio.NioReceiver;

public class Main {

    public static void main(String[] args) throws Exception {
        String contextPath = "" ;
        String appBase = ".";
        Tomcat tomcat = new Tomcat();

        int port = 8080;
        tomcat.setPort(port);
        tomcat.getHost().setAppBase(appBase);
        StandardContext ctx = (StandardContext) tomcat.addWebapp(contextPath, appBase);

        SimpleTcpCluster cluster = new SimpleTcpCluster();
        tomcat.getEngine().setCluster(cluster);
        // Seems like cluster must be added to engine, not context
        //ctx.setCluster(cluster);

        ctx.setName("{CTX}");
        ctx.setDistributable(true);
        ctx.setPrivileged(true);

        cluster.setChannelStartOptions(3);

        DeltaManager manager = new DeltaManager();
        manager.setName("{DELTA}");
        manager.setNotifySessionListenersOnReplication(true);
        cluster.setManagerTemplate(manager);

        GroupChannel channel = (GroupChannel) cluster.getChannel();

        NioReceiver receiver =  new NioReceiver();
        receiver.setPort(9991);
        channel.setChannelReceiver(receiver);

        channel.addInterceptor(new TcpPingInterceptor());
        channel.addInterceptor(new TcpFailureDetector());
        channel.addInterceptor(new MessageDispatchInterceptor());

        StaticMembershipInterceptor membership = new StaticMembershipInterceptor();
        membership.addStaticMember(member(2));
        membership.addStaticMember(member(3));
        membership.addStaticMember(member(4));

        channel.addInterceptor(membership);

        cluster.addValve(new ReplicationValve());
        cluster.addValve(new JvmRouteBinderValve());

        ctx.setManager(manager);

        tomcat.start();
        tomcat.getServer().await();
    }

    private static Member member(int i) {
        StaticMember member = new StaticMember();
        member.setHost("172.17.0." + i);
        member.setPort(9991);
        // Create dummy id = {0, 0, ..., 0, i}
        byte[] id = new byte[16];
        id[15] = (byte) i;
        member.setUniqueId(id);
        return member;
    }

    private static void p(Object o) {
        System.out.println("");
        System.out.println("XXXXXXXXXXX " + o);
        System.out.println("");
    }
}
