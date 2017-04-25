package org.example.tomcat.test;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ServerInfoProvider implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

    private int port;

    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        this.port = event.getEmbeddedServletContainer().getPort();
    }

    public String getInfo() {
        return this.getAddress() + ":" + this.getPort();
    }

    public int getPort() {
        return this.port;
    }

    public String getAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "<unknown>";
        }
    }
}
