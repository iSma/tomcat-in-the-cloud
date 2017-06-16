package org.example.tomcat.cloud;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.example.tomcat.cloud.membership.DynamicMembershipService;
import org.example.tomcat.cloud.membership.KubernetesMemberProvider;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) throws LifecycleException, IOException {
        // Maximum logging
        Enumeration<String> x = LogManager.getLogManager().getLoggerNames();
        while (x.hasMoreElements()) {
            String s = x.nextElement();
            Logger log = LogManager.getLogManager().getLogger(s);
            log.setLevel(Level.FINE);
            for (Handler h : log.getHandlers())
                h.setLevel(Level.FINE);
        }

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
        channel.setMembershipService(new DynamicMembershipService(new KubernetesMemberProvider()));

        channel.addInterceptor(new TcpPingInterceptor());
        channel.addInterceptor(new TcpFailureDetector());
        channel.addInterceptor(new MessageDispatchInterceptor());

        tomcat.start();
        tomcat.getServer().await();
    }
}
