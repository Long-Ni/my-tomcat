package org.example.exchange;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse{

    public final HttpExchange httpExchange;
    byte[] requestBodyData;

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
    public Headers getRequestHeaders() {
        return this.httpExchange.getRequestHeaders();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return this.httpExchange.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return this.httpExchange.getLocalAddress();
    }

    @Override
    public byte[] getRequestBody() throws IOException {
        if (this.requestBodyData == null) {
            try (InputStream input = this.httpExchange.getRequestBody()) {
                this.requestBodyData = input.readAllBytes();
            }
        }
        return this.requestBodyData;
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
