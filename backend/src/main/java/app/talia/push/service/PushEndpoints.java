package app.talia.push.service;

import app.talia.config.TaliaProperties;

import java.net.URI;
import java.util.Locale;

/**
 * SSRF guard: the server only sends requests to the known push services. Checked when a subscription is stored and
 * again before every request.
 */
public final class PushEndpoints {

    private PushEndpoints() {
    }

    public static boolean isAllowed(String endpoint, TaliaProperties.Push push) {
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (Exception e) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        boolean schemeOk = scheme.equals("https") || (push.allowHttp() && scheme.equals("http"));
        if (!schemeOk || uri.getHost() == null || uri.getRawUserInfo() != null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return push.allowedHostSuffixes().stream()
                .map(suffix -> suffix.toLowerCase(Locale.ROOT))
                .anyMatch(suffix -> host.equals(suffix) || host.endsWith("." + suffix));
    }
}
