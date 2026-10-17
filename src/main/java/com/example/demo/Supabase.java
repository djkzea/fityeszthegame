package com.example.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The website's Supabase project, as this game talks to it.
 *
 * The anon key is public by design - the website hands it to every visitor,
 * and row-level security is what protects the data. It still does not belong
 * in git history, so it is read from the environment or from a properties
 * resource that .gitignore keeps out.
 */
public final class Supabase {

    private static final String RESOURCE = "/com/example/demo/supabase.properties";

    private final String url;
    private final String anonKey;
    private final Transport transport;

    public Supabase(String url, String anonKey, Transport transport) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.anonKey = anonKey;
        this.transport = transport;
    }

    /** The configured project, or nothing - in which case the game stays guest only. */
    public static Optional<Supabase> configured() {

        String url = System.getenv("FITYESZ_SUPABASE_URL");
        String key = System.getenv("FITYESZ_SUPABASE_ANON_KEY");

        if (url == null || url.isBlank() || key == null || key.isBlank()) {

            Properties p = new Properties();

            try (InputStream in = Supabase.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    return Optional.empty();
                }
                p.load(in);
            } catch (IOException e) {
                return Optional.empty();
            }

            url = p.getProperty("url", "").trim();
            key = p.getProperty("anonKey", "").trim();
        }

        return url.isBlank() || key.isBlank()
                ? Optional.empty()
                : Optional.of(new Supabase(url, key, new HttpTransport()));
    }

    /**
     * One call against the project. The path starts with a slash; a token, when
     * given, is the signed-in player's.
     */
    public Transport.Res call(String method, String path, String body, String token) throws IOException {
        return call(method, path, body, token, Map.of());
    }

    public Transport.Res call(String method, String path, String body, String token,
                              Map<String, String> extra) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Content-Type", "application/json");

        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }

        headers.putAll(extra);

        return transport.send(method, url + path, headers, body);
    }
}
