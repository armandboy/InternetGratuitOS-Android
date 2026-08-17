package cm.internetgratuit.os;

import java.net.URI;
import java.net.URISyntaxException;

public final class AppConfig {
    private AppConfig() {}

    public static String normalizeBaseUrl(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.isEmpty()) return "";
        if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*") && !value.matches("(?i)^https?://.*")) return "";
        if (!value.matches("(?i)^https?://.*")) value = "http://" + value;
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        try {
            URI uri = new URI(value);
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) return "";
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return "";
            return value;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    public static String route(String baseUrl, String path) {
        String base = normalizeBaseUrl(baseUrl);
        if (base.isEmpty()) return "";
        if (path == null || path.isEmpty()) return base;
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
