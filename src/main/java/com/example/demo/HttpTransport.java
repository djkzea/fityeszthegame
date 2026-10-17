package com.example.demo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** The real network: one client, short timeouts, no retries of its own. */
public final class HttpTransport implements Transport {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @Override
    public Res send(String method, String url, Map<String, String> headers, String body) throws IOException {

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT);

        headers.forEach(b::header);

        b.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));

        try {
            HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Res(res.statusCode(), res.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
