package org.example;

import com.sun.net.httpserver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class MyHttpServer implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());
    final HttpServer httpServer;

    public MyHttpServer(String host, int port) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0, "/", this);
        this.httpServer.start();
        logger.info("start my-http-server at {}:{}", host, port);
    }

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;

        try (MyHttpServer connector = new MyHttpServer(host, port)) {
            connector.logger.info("please access: http://localhost:8080");
            for (;;) {
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 获取请求method、path、query
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String query = uri.getQuery();
        logger.info("method:{}. path:{}. query:{}", method, path, query);

        // 输出响应
        // 设置响应header
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html;charset=utf-8");
        headers.set("Cache-Control", "no-cache");
        // 设置响应code
        exchange.sendResponseHeaders(200, 0);

        // 写入响应内容
        String respContent = "<h1>Hello, world.</h1><p>" + LocalDateTime.now().withNano(0) + "</p>";
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(respContent.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void close() throws Exception {
        this.httpServer.stop(3);
    }
}
