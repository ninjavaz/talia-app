package app.talia.push.model;

import java.time.Duration;

/**
 * A notification. {@code tag} replaces an earlier notification with the same tag on the device; {@code url} is opened
 * when it is tapped; {@code ttl} is how long the push service keeps it for an offline device.
 */
public record PushMessage(String title, String body, String url, String tag, Duration ttl) {
}
