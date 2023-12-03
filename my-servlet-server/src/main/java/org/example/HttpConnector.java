package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.exchange.HttpExchangeAdapter;
import org.example.servlet.HttpServletRequestImpl;
import org.example.servlet.HttpServletResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

public class HttpConnector implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpServer httpServer;

    public HttpConnector() throws IOException {
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
            process(requestAdapter, responseAdapter);
        } catch (IOException e) {
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
