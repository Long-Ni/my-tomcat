package org.example.exchange;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.engine.HttpServletRequestImpl;
import org.example.engine.HttpServletResponseImpl;
import org.example.engine.ServletContextImpl;
import org.example.engine.servlet.HelloServlet;
import org.example.engine.servlet.IndexServlet;
import org.example.exchange.HttpExchangeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpServer httpServer;
    final ServletContextImpl servletContext;
    final Duration stopDelay = Duration.ofSeconds(5);

    public HttpConnector() throws IOException {
        this.servletContext = new ServletContextImpl();
        this.servletContext.initialize(List.of(IndexServlet.class, HelloServlet.class));

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
        var requestAdapter = new HttpServletRequestImpl(adapter);
        var responseAdapter = new HttpServletResponseImpl(adapter);

        try {
            this.servletContext.process(requestAdapter, responseAdapter);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 这个方法内部就可以按照Servlet标准来处理HTTP请求了
     * 因为方法参数是标准的Servlet接口
     *
     */
    private void process(HttpServletRequestImpl request, HttpServletResponseImpl response) throws IOException {
        // 获取请求
        String name = request.getParameter("name");

        // 响应体
        String respBody = "<h1>Hello, " + (name == null ? "world" : name) + ".</h1>";

        // 设置响应
        response.setContentType("text/html");
        PrintWriter pw = response.getWriter();
        pw.write(respBody);
        pw.close();
    }

    @Override
    public void close() throws Exception {
        this.httpServer.stop(3);
    }
}
