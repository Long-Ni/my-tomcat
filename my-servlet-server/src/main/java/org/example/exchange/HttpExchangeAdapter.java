package org.example.exchange;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse{

    final HttpExchange httpExchange;

    public HttpExchangeAdapter(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public String getRequestMethod() {
        return this.httpExchange.getRequestMethod();
    }

    @Override
    public URI getRequestURI() {
        return this.httpExchange.getRequestURI();
    }

    @Override
    public Headers getResponseHeader() {
        return this.httpExchange.getResponseHeaders();
    }

    @Override
    public void sendResponseHeaders(int responseCode, long responseLength) throws IOException {
        this.httpExchange.sendResponseHeaders(responseCode, responseLength);
    }

    @Override
    public OutputStream getResponseBody() {
        return this.httpExchange.getResponseBody();
    }
}
