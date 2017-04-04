package org.example.tomcat.test;

import org.apache.catalina.Context;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfiguration {
    @Bean
    public EmbeddedServletContainerFactory servletContainerFactory() {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.addContextCustomizers(new ContextCustomizer());
        return factory;
    }

    private static class ContextCustomizer implements TomcatContextCustomizer {
        @Override
        public void customize(Context context) {
            context.setDistributable(true);
            // context.setManager(new MyManager());
        }
    }
}

