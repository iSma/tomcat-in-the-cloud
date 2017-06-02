package org.example.kubeping;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.McastService;

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

        GroupChannel channel = (GroupChannel) cluster.getChannel();
        channel.setMembershipService(new KubeshipService());

        channel.addInterceptor(new TcpPingInterceptor());
        channel.addInterceptor(new TcpFailureDetector());
        channel.addInterceptor(new MessageDispatchInterceptor());

        tomcat.start();
        tomcat.getServer().await();
    }
}
