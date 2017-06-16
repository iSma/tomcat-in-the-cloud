package org.example.tomcat.cloud;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.MembershipService;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.example.tomcat.cloud.membership.DynamicMembershipService;
import org.example.tomcat.cloud.membership.KubernetesMemberProvider;
import org.example.tomcat.cloud.membership.MemberProvider;

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

        // Embedded Tomcat Configuration:

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080); // HTTP port for Tomcat; make sure to set the same value in pom.xml

        // Servlet Configuration:
        File base = new File(System.getProperty("java.io.tmpdir"));
        Context ctx = tomcat.addContext("", base.getAbsolutePath());
        Tomcat.addServlet(ctx, "TestServlet", new TestServlet());
        ctx.addServletMappingDecoded("/", "TestServlet");

        // Cluster configuration
        SimpleTcpCluster cluster = new SimpleTcpCluster();
        tomcat.getEngine().setCluster(cluster);

        ctx.setDistributable(true);

        GroupChannel channel = (GroupChannel) cluster.getChannel();

        // The interesting part: use DynamicMembershipService (with KubernetesMemberProvider)
        MemberProvider provider = new KubernetesMemberProvider();
        MembershipService service = new DynamicMembershipService(provider);
        channel.setMembershipService(service);

        // Start Tomcat
        tomcat.start();
        tomcat.getServer().await();
    }
}
