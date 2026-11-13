package app.talia.sync;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Parses ISO-like date-times coming from iOS Shortcuts into local time of the user's timezone. */
final class FlexibleDateTime {

    private FlexibleDateTime() {
    }

    static LocalDateTime parse(String raw, ZoneId zone) {
        String s = raw.trim().replace(' ', 'T');
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(zone).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ignored) {
            // no offset, try local
        }
        try {
            return LocalDateTime.parse(s).truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException e) {
            throw new InvalidSyncDataException("Unparseable date-time: " + raw);
        }
    }
}
