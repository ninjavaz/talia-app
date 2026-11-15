package app.talia.push.service;

import java.util.Map;

/** Notification texts in the user's language (PL default, EN). */
final class PushTexts {

    private static final Map<String, Map<String, String>> TEXTS = Map.of(
            "pl", Map.of(
                    "title", "Talia",
                    "test", "Powiadomienia działają na tym urządzeniu.",
                    "habit", "Pora na: %s",
                    "steps", "Kroki: %s",
                    "checkin.title", "Czas na cotygodniowy pomiar",
                    "checkin.body", "Obwody, waga i średnia kroków z tygodnia. Zajmie minutę.",
                    "streak.title", "Nie przerwij serii (%d)",
                    "today.title", "Nawyki na dziś",
                    "streak.body", "Do odhaczenia: %d z %d."),
            "en", Map.of(
                    "title", "Talia",
                    "test", "Notifications work on this device.",
                    "habit", "Time for: %s",
                    "steps", "Steps: %s",
                    "checkin.title", "Time for the weekly check-in",
                    "checkin.body", "Measurements, weight and the week's average steps. Takes a minute.",
                    "streak.title", "Keep your streak (%d)",
                    "today.title", "Today's habits",
                    "streak.body", "Left to check: %d of %d."));

    private PushTexts() {
    }

    static String get(String language, String key, Object... args) {
        Map<String, String> texts = TEXTS.getOrDefault(language, TEXTS.get("pl"));
        return args.length == 0 ? texts.get(key) : texts.get(key).formatted(args);
    }

    /** 8000 -> "8 000" / "8,000", like the app shows the steps habit. */
    static String thousands(String language, int value) {
        String separator = "en".equals(language) ? "," : " ";
        return String.valueOf(value).replaceAll("\\B(?=(\\d{3})+(?!\\d))", separator);
    }
}
