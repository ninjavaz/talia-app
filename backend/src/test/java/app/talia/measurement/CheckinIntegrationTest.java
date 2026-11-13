package app.talia.measurement;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckinIntegrationTest extends IntegrationTest {

    private ResultActions checkin(String json) throws Exception {
        return mvc.perform(put("/api/checkins").with(asUser()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());
    }

    @Test
    void savesAllValuesOfAWeek() throws Exception {
        checkin("""
                {"date": "2026-11-15", "waistCm": 78.5, "weightKg": 64.2, "avgDailySteps": 8100}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-11-09"))
                .andExpect(jsonPath("$.waistCm").value(78.5))
                .andExpect(jsonPath("$.weightKg").value(64.2))
                .andExpect(jsonPath("$.avgDailySteps").value(8100));

        assertThat(jdbc.sql("SELECT source FROM weight_entry").query(String.class).single()).isEqualTo("MANUAL");
    }

    @Test
    void secondCheckinInTheSameWeekReplacesTheFirst() throws Exception {
        checkin("""
                {"date": "2026-11-14", "waistCm": 79, "weightKg": 64.6, "avgDailySteps": 7000}
                """).andExpect(status().isOk());
        checkin("""
                {"date": "2026-11-15", "waistCm": 78.5, "weightKg": 64.2, "avgDailySteps": 8100}
                """).andExpect(status().isOk());

        assertThat(count("waist_entry")).isEqualTo(1);
        assertThat(count("weight_entry")).isEqualTo(1);
        assertThat(count("weekly_steps")).isEqualTo(1);
        assertThat(jdbc.sql("SELECT avg_daily_steps FROM weekly_steps").query(Integer.class).single()).isEqualTo(8100);
    }

    @Test
    void missingValuesLeaveThatMetricUntouched() throws Exception {
        checkin("""
                {"date": "2026-11-15", "waistCm": 78.5, "weightKg": 64.2}
                """).andExpect(status().isOk());
        checkin("""
                {"date": "2026-11-15", "avgDailySteps": 9000}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waistCm").value(78.5))
                .andExpect(jsonPath("$.avgDailySteps").value(9000));
    }

    @Test
    void rejectsEmptyFutureAndOutOfRangeCheckins() throws Exception {
        checkin("""
                {"date": "2026-11-15"}
                """).andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-16", "waistCm": 78}
                """).andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-15", "waistCm": 5}
                """).andExpect(status().isBadRequest());
        assertThat(count("waist_entry")).isZero();
    }

    @Test
    void progressShowsDeltasSinceStartAndHistory() throws Exception {
        checkin("""
                {"date": "2026-11-01", "waistCm": 80.0, "weightKg": 65.0, "avgDailySteps": 6000}
                """).andExpect(status().isOk());
        checkin("""
                {"date": "2026-11-15", "waistCm": 78.5, "weightKg": 64.0, "avgDailySteps": 8100}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/progress").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waist.latest").value(78.5))
                .andExpect(jsonPath("$.waist.first").value(80.0))
                .andExpect(jsonPath("$.waist.delta").value(-1.5))
                .andExpect(jsonPath("$.weight.delta").value(-1.0))
                .andExpect(jsonPath("$.waistPoints.length()").value(2))
                .andExpect(jsonPath("$.avgDailyStepsThisWeek").value(8100))
                .andExpect(jsonPath("$.dailyStepsGoal").value(8000))
                .andExpect(jsonPath("$.checkinDoneThisWeek").value(true))
                .andExpect(jsonPath("$.checkinDue").value(false));

        mvc.perform(get("/api/checkins").with(asUser()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weekStart").value("2026-11-09"))
                .andExpect(jsonPath("$[1].weekStart").value("2026-10-26"));
    }

    @Test
    void checkinIsDueOnSundayWhenNotDoneThisWeek() throws Exception {
        checkin("""
                {"date": "2026-11-08", "waistCm": 79}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/progress").with(asUser()))
                .andExpect(jsonPath("$.checkinDoneThisWeek").value(false))
                .andExpect(jsonPath("$.checkinDue").value(true))
                .andExpect(jsonPath("$.lastCheckin").value("2026-11-08"));
    }

    @Test
    void emptyProgressIsDue() throws Exception {
        mvc.perform(get("/api/progress").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waist").doesNotExist())
                .andExpect(jsonPath("$.checkinDue").value(true));
    }

    @Test
    void deletingAWeekKeepsSyncedWeight() throws Exception {
        checkin("""
                {"date": "2026-11-15", "waistCm": 78.5, "weightKg": 64.2, "avgDailySteps": 8100}
                """).andExpect(status().isOk());
        jdbc.sql("INSERT INTO weight_entry (measured_at, value_kg, source) VALUES ('2026-11-12 07:10', 64.4, 'HEALTH')")
                .update();

        mvc.perform(delete("/api/checkins/2026-11-11").with(asUser())).andExpect(status().isNoContent());

        assertThat(count("waist_entry")).isZero();
        assertThat(count("weekly_steps")).isZero();
        assertThat(jdbc.sql("SELECT source FROM weight_entry").query(String.class).list()).containsExactly("HEALTH");
    }

    @Test
    void movingAverageUsesTheLastSevenDays() {
        var points = java.util.List.of(
                new CheckinDtos.Point(java.time.LocalDate.parse("2026-11-01"), new java.math.BigDecimal("65")),
                new CheckinDtos.Point(java.time.LocalDate.parse("2026-11-03"), new java.math.BigDecimal("64")),
                new CheckinDtos.Point(java.time.LocalDate.parse("2026-11-10"), new java.math.BigDecimal("63")));
        var trend = CheckinService.movingAverage(points, 7);
        assertThat(trend.get(1).value()).isEqualByComparingTo("64.5");
        assertThat(trend.get(2).value()).isEqualByComparingTo("63");
    }
}
