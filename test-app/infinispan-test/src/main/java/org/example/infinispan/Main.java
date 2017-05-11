package org.example.infinispan;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.jgroups.logging.LogFactory;
import org.jgroups.ping.kube.Client;
import org.jgroups.ping.kube.KubePing;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    public static Cache<String, Integer> cache;
    public static String SERVER;

    public static void main(String[] args) throws LifecycleException, IOException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        //LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.FINE);
        //for (Handler h : LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).getHandlers())
        //    h.setLevel(Level.FINE);


        SERVER = InetAddress.getLocalHost().getHostAddress();

        File base = new File(System.getProperty("java.io.tmpdir"));
        Context ctx = tomcat.addContext("", base.getAbsolutePath());
        Tomcat.addServlet(ctx, "TestServlet", new TestServlet());
        ctx.addServletMappingDecoded("/", "TestServlet");

        GlobalConfiguration globalConfig = new GlobalConfigurationBuilder().transport()
                .defaultTransport()
                .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml")
                .build();

        ConfigurationBuilder cacheConfiguration = new ConfigurationBuilder();
        cacheConfiguration.clustering().cacheMode(CacheMode.DIST_SYNC);

        DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfig, cacheConfiguration.build());
        cache = cacheManager.getCache();
        cache.addListener(new MyListener());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> cache.put(SERVER, 666),
                0, 2, TimeUnit.SECONDS);
/*
        Enumeration<String> x = LogManager.getLogManager().getLoggerNames();
        while(x.hasMoreElements()) {
            String s = x.nextElement();
            Logger log = LogManager.getLogManager().getLogger(s);
            //Logger.getLogger("Main").warning("[" + s + "] level = " + log.getLevel());
            log.setLevel(Level.ALL);
            for (Handler h : log.getHandlers())
                h.setLevel(Level.ALL);
        }

        System.out.println("XXX level = " + LogFactory.getLog(KubePing.class).getLevel());
        System.out.println("XXX debug = " + LogFactory.getLog(KubePing.class).isDebugEnabled());
*/
        tomcat.start();
        tomcat.getServer().await();

        scheduler.shutdown();
        cacheManager.stop();
    }

    @Listener
    public static class MyListener {

        @CacheEntryCreated
        public void entryCreated(CacheEntryCreatedEvent<String, Object> event) {
            if (!event.isPre())
                System.out.println("[NEW] <" + SERVER + "> " + event.getKey() + "=" + event.getValue());
        }

        @CacheEntryModified
        public void entryModified(CacheEntryModifiedEvent<String, Object> event) {
            if (!event.isPre())
                System.out.println("[UPD] <" + SERVER + "> " + event.getKey() + "=" + event.getValue());
                //System.out.println();
        }
    }
}
