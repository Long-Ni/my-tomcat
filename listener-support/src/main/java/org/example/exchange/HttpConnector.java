package org.example.exchange;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.engine.HttpServletRequestImpl;
import org.example.engine.HttpServletResponseImpl;
import org.example.engine.ServletContextImpl;
import org.example.engine.filter.HelloFilter;
import org.example.engine.filter.LogFilter;
import org.example.engine.listener.*;
import org.example.engine.servlet.HelloServlet;
import org.example.engine.servlet.IndexServlet;
import org.example.engine.servlet.LoginServlet;
import org.example.engine.servlet.LogoutServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.EventListener;
import java.util.List;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpServer httpServer;
    final ServletContextImpl servletContext;
    final Duration stopDelay = Duration.ofSeconds(5);

    public HttpConnector() throws IOException {
        this.servletContext = new ServletContextImpl();
        // 注册所有 Servlet
        this.servletContext.initServlets(List.of(IndexServlet.class, HelloServlet.class, LoginServlet.class, LogoutServlet.class));
        // 注册所有 Filter
        this.servletContext.initFilters(List.of(LogFilter.class, HelloFilter.class));
        // 注册所有的 Listener
        List<Class<? extends EventListener>> listenerClasses = List.of(
                CustomHttpSessionAttributeListener.class,
                CustomHttpSessionListener.class,
                CustomServletContextAttributeListener.class,
                CustomServletContextListener.class,
                CustomServletRequestAttributeListener.class,
                CustomServletRequestListener.class
        );
        for (Class<? extends EventListener> listenerClass : listenerClasses) {
            this.servletContext.addListener(listenerClass);
        }

        // start http server:
        String host = "0.0.0.0";
        int port = 8080;

        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0, "/", this);
        this.httpServer.start();
        logger.info("start my-http-server at {}:{}", host, port);
        logger.info("please access: http://localhost:8080");
    }

    @Override
    public void handle(HttpExchange exchange) {

        logger.info("{}: {}?{}", exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getRequestURI().getRawQuery());

        var adapter = new HttpExchangeAdapter(exchange);
        var responseAdapter = new HttpServletResponseImpl(adapter);
        var requestAdapter = new HttpServletRequestImpl(adapter, this.servletContext, responseAdapter);

        try {
            this.servletContext.process(requestAdapter, responseAdapter);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        this.httpServer.stop((int) this.stopDelay.toSeconds());
    }
}
