package app.talia.habit;

import app.talia.habit.service.StreakRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** REQ-015 without the database: which day counts and how the streak is counted. */
class StreakRuleTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-15");

    /** Completed days as a set; every other day is not completed. */
    private static int streak(LocalDate... completedDays) {
        Set<LocalDate> days = Set.of(completedDays);
        return StreakRule.streak(TODAY, days::contains);
    }

    @Test
    @DisplayName("REQ-015 a day counts only when every active habit is done")
    void dayCountsWhenAllActiveHabitsAreDone() {
        assertThat(StreakRule.isCompleted(Set.of(1L, 2L, 3L), Set.of(1L, 2L, 3L))).isTrue();
        assertThat(StreakRule.isCompleted(Set.of(1L, 2L, 3L), Set.of(1L, 3L))).isFalse();
        // a check of a habit that was not active that day does not help
        assertThat(StreakRule.isCompleted(Set.of(1L, 2L), Set.of(1L, 4L))).isFalse();
        assertThat(StreakRule.isCompleted(Set.of(1L, 2L), Set.of(1L, 2L, 4L))).isTrue();
    }

    @Test
    @DisplayName("REQ-015 a day without active habits does not count")
    void dayWithoutHabitsDoesNotCount() {
        assertThat(StreakRule.isCompleted(Set.of(), Set.of())).isFalse();
    }

    @Test
    @DisplayName("REQ-015 consecutive completed days before an incomplete today")
    void incompleteTodayDoesNotBreakTheStreak() {
        assertThat(streak(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3), TODAY.minusDays(5)))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("REQ-015 a completed today extends the streak")
    void completedTodayCounts() {
        assertThat(streak(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3))).isEqualTo(4);
        assertThat(streak(TODAY)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-015 a missed day breaks the streak")
    void missedDayBreaksTheStreak() {
        assertThat(streak(TODAY.minusDays(2), TODAY.minusDays(3))).isZero();
        assertThat(streak(TODAY, TODAY.minusDays(2))).isEqualTo(1);
        assertThat(streak()).isZero();
    }

    @Test
    @DisplayName("REQ-015 the streak is capped")
    void streakIsCapped() {
        assertThat(StreakRule.streak(TODAY, day -> true)).isEqualTo(StreakRule.MAX_DAYS);
    }
}
