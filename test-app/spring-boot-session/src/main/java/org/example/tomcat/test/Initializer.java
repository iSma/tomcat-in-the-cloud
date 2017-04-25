package org.example.tomcat.test;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

/**
 * Created by hal on 25.04.17.
 */

public class Initializer extends AbstractHttpSessionApplicationInitializer {

    public Initializer() {
        super(HttpSessionConfig.class);
    }
}