package org.example.tomcat.test;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.ha.session.ClusterSessionListener;
import org.apache.catalina.ha.session.DeltaManager;
import org.apache.catalina.ha.session.JvmRouteBinderValve;
import org.apache.catalina.ha.tcp.ReplicationValve;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor;
import org.apache.catalina.tribes.membership.StaticMember;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.transport.nio.NioReceiver;
import org.apache.catalina.tribes.transport.nio.PooledParallelSender;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.apache.catalina.tribes.group.interceptors.MessageDispatchInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfiguration
{
    @Bean
    public EmbeddedServletContainerFactory servletContainerFactory()
    {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory()
        {
            @Override
            protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(
                    Tomcat tomcat)
            {
                configureCluster(tomcat);
                return super.getTomcatEmbeddedServletContainer(tomcat);
            }

            private void configureCluster(Tomcat tomcat)
            {
                // static membership cluster

                SimpleTcpCluster cluster = new SimpleTcpCluster();
                cluster.setChannelStartOptions(3);
                {
                    DeltaManager manager = new DeltaManager();
                    manager.setNotifyListenersOnReplication(true);
                    cluster.setManagerTemplate(manager);
                }
                {
                    GroupChannel channel = new GroupChannel();
                    {
                        NioReceiver receiver = new NioReceiver();
                        receiver.setPort(localClusterMemberPort);
                        channel.setChannelReceiver(receiver);
                    }
                    {
                        ReplicationTransmitter sender = new ReplicationTransmitter();
                        sender.setTransport(new PooledParallelSender());
                        channel.setChannelSender(sender);
                    }
                    channel.addInterceptor(new TcpPingInterceptor());
                    channel.addInterceptor(new TcpFailureDetector());
                    channel.addInterceptor(new MessageDispatchInterceptor());
                    {
                        StaticMembershipInterceptor membership =
                                new StaticMembershipInterceptor();
                        String[] memberSpecs = clusterMembers.split(",", -1);
                        for (String spec : memberSpecs)
                        {
                            ClusterMemberDesc memberDesc = new ClusterMemberDesc(spec);
                            StaticMember member = new StaticMember();
                            member.setHost(memberDesc.address);
                            member.setPort(memberDesc.port);
                            member.setDomain("MyWebAppDomain");
                            member.setUniqueId(memberDesc.uniqueId);
                            membership.addStaticMember(member);
                        }
                        channel.addInterceptor(membership);
                    }
                    cluster.setChannel(channel);
                }
                cluster.addValve(new ReplicationValve());
                cluster.addValve(new JvmRouteBinderValve());
                cluster.addClusterListener(new ClusterSessionListener());

                tomcat.getEngine().setCluster(cluster);
            }

        };

        factory.addContextCustomizers(new TomcatContextCustomizer()
        {
            @Override
            public void customize(Context context)
            {
                context.setManager(new DeltaManager());
                context.setDistributable(true);
            }
        });

        return factory;
    }

    private static class ClusterMemberDesc
    {
        public String address;
        public int port;
        public String uniqueId;

        public ClusterMemberDesc(String spec) throws IllegalArgumentException
        {
            String[] values = spec.split(":", -1);
            if (values.length != 3)
                throw new IllegalArgumentException("clusterMembers element " +
                        "format must be address:port:uniqueIndex");
            address = values[0];
            port = Integer.parseInt(values[1]);
            int index = Integer.parseInt(values[2]);
            if ((index < 0) || (index > 255))
                throw new IllegalArgumentException("invalid unique index: must be >= 0 and < 256");
            uniqueId = "{";
            for (int i = 0; i < 16; i++, index++)
            {
                if (i != 0)
                    uniqueId += ',';
                uniqueId += index % 256;
            }
            uniqueId += '}';
        }
    };

    // This is for example. In fact these are read from application.properties
    private int localClusterMemberPort = 9992;
    private String clusterMembers = "172.17.0.2:9992:1,172.17.0.3:9992:2,172.17.0.4:9992:3";
}


