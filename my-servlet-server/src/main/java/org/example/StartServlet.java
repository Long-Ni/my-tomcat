package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartServlet {

    static Logger logger = LoggerFactory.getLogger(StartServlet.class);

    public static void main(String[] args) {

        try (HttpConnector httpConnector = new HttpConnector()) {
            for (;;) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("my-servlet-server was shutdown.");
    }
}