package org.example.tomcat.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.net.UnknownHostException;

@RestController
public class Controller {

    @Autowired
    ServerInfoProvider serverInfoProvider;

	@Autowired
    private HttpSession session;

    @RequestMapping("/")
    public SessionInfo index() {
        updateSession();
        return new SessionInfo(session, serverInfoProvider.getInfo());
    }

    private HttpSession updateSession() {
        Integer counter = (Integer) session.getAttribute("counter");

        if (counter == null) {
            counter = 1;
        } else {
            counter++;
        }

        session.setAttribute("counter", counter);
        return session;
    }
}
