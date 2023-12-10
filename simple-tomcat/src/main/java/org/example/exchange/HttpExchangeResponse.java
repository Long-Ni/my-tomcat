package org.example.exchange;

import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpExchangeResponse {

    void sendResponseHeaders(int responseCode, long responseLength) throws IOException;

    OutputStream getResponseBody();

    Headers getResponseHeaders();
}
