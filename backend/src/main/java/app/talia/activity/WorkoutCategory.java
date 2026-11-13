package app.talia.activity;

import java.util.List;
import java.util.Locale;

/**
 * Coarse workout category derived from the Apple Health workout type name. The Shortcut may send English
 * or Polish names depending on the phone's language, so both are matched.
 */
public enum WorkoutCategory {
    GYM, PILATES, WALK, OTHER;

    private static final List<String> GYM_KEYWORDS =
            List.of("strength", "siłow", "silow", "functional", "funkcjonaln", "hiit", "gym", "core");
    private static final List<String> WALK_KEYWORDS =
            List.of("walk", "spacer", "chód", "chod", "hiking", "wędrówk", "wedrowk");

    public static WorkoutCategory fromHealthType(String type) {
        String t = type.toLowerCase(Locale.ROOT);
        if (t.contains("pilates")) {
            return PILATES;
        }
        if (GYM_KEYWORDS.stream().anyMatch(t::contains)) {
            return GYM;
        }
        if (WALK_KEYWORDS.stream().anyMatch(t::contains)) {
            return WALK;
        }
        return OTHER;
    }
}
