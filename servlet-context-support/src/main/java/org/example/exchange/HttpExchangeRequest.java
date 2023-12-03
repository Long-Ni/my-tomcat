package org.example.exchange;

import java.net.URI;

public interface HttpExchangeRequest {

    String getRequestMethod();

    URI getRequestURI();
}
