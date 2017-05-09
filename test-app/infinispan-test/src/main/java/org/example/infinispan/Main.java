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

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static Cache<String, Integer> cache;
    public static String SERVER;

    public static void main(String[] args) throws LifecycleException, IOException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

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
        }
    }
}
