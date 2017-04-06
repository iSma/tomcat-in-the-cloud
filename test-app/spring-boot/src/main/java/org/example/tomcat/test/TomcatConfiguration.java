package org.example.tomcat.test;

import org.apache.catalina.Context;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfiguration {
    private static final int CLUSTER_PORT = 9991;

    @Bean
    public EmbeddedServletContainerFactory servletContainerFactory() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        /*
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory() {
            @Override
            protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(Tomcat tomcat) {
                // TODO: doesn't work
                SimpleTcpCluster cluster = new SimpleTcpCluster();
                cluster.setChannelStartOptions(3);
                DeltaManager manager = new DeltaManager();
                manager.setNotifyListenersOnReplication(true);
                cluster.setManagerTemplate(manager);
                GroupChannel channel = new GroupChannel();
                NioReceiver receiver = new NioReceiver();
                receiver.setPort(CLUSTER_PORT);

                channel.setChannelReceiver(receiver);
                ReplicationTransmitter sender = new ReplicationTransmitter();
                sender.setTransport(new PooledParallelSender());
                channel.setChannelSender(sender);
                channel.addInterceptor(new TcpPingInterceptor());
                channel.addInterceptor(new TcpFailureDetector());

                StaticMembershipInterceptor membership = new StaticMembershipInterceptor();

                for (int i = 2; i <= 4; i++) {
                    StaticMember member = new StaticMember();
                    member.setHost("172.0.0." + i);
                    member.setPort(CLUSTER_PORT);
                    member.setDomain("MyWebAppDomain");
                    membership.addStaticMember(member);
                }

                channel.addInterceptor(membership);
                cluster.setChannel(channel);
                cluster.addValve(new ReplicationValve());
                cluster.addValve(new JvmRouteBinderValve());
                cluster.addClusterListener(new ClusterSessionListener());

                tomcat.getEngine().setCluster(cluster);

                return super.getTomcatEmbeddedServletContainer(tomcat);
            }
        };
        */

        factory.addContextCustomizers(new ContextCustomizer());
        return factory;
    }

    private static class ContextCustomizer implements TomcatContextCustomizer {

        @Override
        public void customize(Context context) {
            // context.setManager(new DeltaManager());
            context.setDistributable(true);
        }
    }
}

