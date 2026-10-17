package com.example.demo;

import java.io.IOException;
import java.util.Map;

/**
 * The whole of the network, in one method.
 *
 * Everything above this talks in requests and answers, so a test can hand the
 * account layer canned replies instead of standing up a server.
 */
public interface Transport {

    record Res(int status, String body) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    Res send(String method, String url, Map<String, String> headers, String body) throws IOException;
}
