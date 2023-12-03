package org.example;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class MyHttpHandle implements HttpHandler {

    final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * HttpExchange封装了HTTP请求和响应：可以读取HTTP请求的输入，并将HTTP响应输出给它。
     * 使用HttpExchange无需解析原始的HTTP请求，也无需构造原始的HTTP响应。
     * 而是通过HttpExchange间接操作，大大简化了HTTP请求的处理。
     */
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
}
