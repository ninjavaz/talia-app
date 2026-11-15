package app.talia.push.service;

import app.talia.config.TaliaProperties;
import app.talia.push.entity.PushSubscriptionEntity;
import app.talia.push.model.PushMessage;
import app.talia.push.model.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Sends a Web Push message: RFC 8291 encryption, RFC 8292 VAPID, RFC 8030 HTTP request. */
@Component
public class WebPushClient implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushClient.class);
    private static final String ICON = "/icons/icon-192x192.png";

    private final Vapid vapid;
    private final TaliaProperties.Push push;
    private final ObjectMapper json;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Autowired
    WebPushClient(Vapid vapid, TaliaProperties properties, ObjectMapper json, Clock clock) {
        this(vapid, properties.push(), json, clock);
    }

    public WebPushClient(Vapid vapid, TaliaProperties.Push push, ObjectMapper json, Clock clock) {
        this.vapid = vapid;
        this.push = push;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public SendResult send(PushSubscriptionEntity subscription, PushMessage message) {
        if (!PushEndpoints.isAllowed(subscription.getEndpoint(), push)) {
            log.warn("Push endpoint of subscription {} is not an allowed push service; not sent", subscription.getId());
            return SendResult.FAILED;
        }
        Base64.Decoder b64 = Base64.getUrlDecoder();
        byte[] body = WebPushCrypto.encrypt(payload(message), b64.decode(subscription.getP256dh()),
                b64.decode(subscription.getAuth()));
        HttpRequest request = HttpRequest.newBuilder(URI.create(subscription.getEndpoint()))
                .timeout(Duration.ofSeconds(15))
                .header("TTL", String.valueOf(message.ttl().toSeconds()))
                .header("Urgency", "normal")
                .header("Content-Type", "application/octet-stream")
                .header("Content-Encoding", "aes128gcm")
                .header("Authorization", vapid.authorization(subscription.getEndpoint(), clock.instant()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                return SendResult.OK;
            }
            if (status == 404 || status == 410) {
                return SendResult.GONE;
            }
            log.warn("Push service answered {} for subscription {}", status, subscription.getId());
            return SendResult.FAILED;
        } catch (IOException e) {
            log.warn("Push request for subscription {} failed: {}", subscription.getId(), e.getMessage());
            return SendResult.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.FAILED;
        }
    }

    /** The Angular service worker shows {@code notification} and opens {@code url} on a tap. */
    private byte[] payload(PushMessage message) {
        Map<String, Object> notification = Map.of(
                "title", message.title(),
                "body", message.body(),
                "icon", ICON,
                "tag", message.tag(),
                "data", Map.of("onActionClick", Map.of("default",
                        Map.of("operation", "navigateLastFocusedOrOpen", "url", message.url()))));
        return json.writeValueAsBytes(Map.of("notification", notification));
    }
}
