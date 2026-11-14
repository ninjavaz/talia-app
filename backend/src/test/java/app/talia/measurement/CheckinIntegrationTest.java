package app.talia.measurement;

import app.talia.IntegrationTest;
import app.talia.measurement.model.Point;
import app.talia.measurement.service.CheckinService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Body part 1 is talia's "Talia" (see IntegrationTest). */
class CheckinIntegrationTest extends IntegrationTest {

    private ResultActions checkin(String json) throws Exception {
        return mvc.perform(put("/api/checkins").with(asUser()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void addPart(String name) throws Exception {
        mvc.perform(post("/api/body-parts").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated());
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
        addPart("Biceps");
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78.5}, {"partId": 2, "valueCm": 28}],
                 "weightKg": 64.2, "avgDailySteps": 8100}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-11-09"))
                .andExpect(jsonPath("$.measurements.length()").value(2))
                .andExpect(jsonPath("$.measurements[0].partId").value(1))
                .andExpect(jsonPath("$.measurements[0].valueCm").value(78.5))
                .andExpect(jsonPath("$.measurements[1].valueCm").value(28.0))
                .andExpect(jsonPath("$.weightKg").value(64.2))
                .andExpect(jsonPath("$.avgDailySteps").value(8100));

        assertThat(jdbc.sql("SELECT source FROM weight_entry").query(String.class).single()).isEqualTo("MANUAL");
    }

    @Test
    void secondCheckinInTheSameWeekReplacesTheFirst() throws Exception {
        addPart("Udo");
        checkin("""
                {"date": "2026-11-14", "measurements": [{"partId": 1, "valueCm": 79}, {"partId": 2, "valueCm": 55}],
                 "weightKg": 64.6, "avgDailySteps": 7000}
                """).andExpect(status().isOk());
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78.5}], "weightKg": 64.2}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements.length()").value(1))
                .andExpect(jsonPath("$.avgDailySteps").doesNotExist());

        assertThat(count("body_measurement")).isEqualTo(1);
        assertThat(count("weight_entry")).isEqualTo(1);
        assertThat(count("weekly_steps")).isZero();
    }

    @Test
    void rejectsEmptyFutureOutOfRangeAndUnknownParts() throws Exception {
        checkin("{\"date\": \"2026-11-15\"}").andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-16", "measurements": [{"partId": 1, "valueCm": 78}]}
                """).andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 2}]}
                """).andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 99, "valueCm": 30}]}
                """).andExpect(status().isBadRequest());
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78}, {"partId": 1, "valueCm": 79}]}
                """).andExpect(status().isBadRequest());
        assertThat(count("body_measurement")).isZero();
    }

    @Test
    void progressShowsDeltasPerBodyPartAndHistory() throws Exception {
        addPart("Biceps");
        checkin("""
                {"date": "2026-11-01", "measurements": [{"partId": 1, "valueCm": 80.0}, {"partId": 2, "valueCm": 29}],
                 "weightKg": 65.0, "avgDailySteps": 6000}
                """).andExpect(status().isOk());
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78.5}, {"partId": 2, "valueCm": 28.5}],
                 "weightKg": 64.0, "avgDailySteps": 8100}
                """).andExpect(status().isOk());

        mvc.perform(get("/api/progress").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyParts[0].metric.latest").value(78.5))
                .andExpect(jsonPath("$.bodyParts[0].metric.delta").value(-1.5))
                .andExpect(jsonPath("$.weight.delta").value(-1.0))
                .andExpect(jsonPath("$.bodyParts.length()").value(2))
                .andExpect(jsonPath("$.bodyParts[0].name").value("Talia"))
                .andExpect(jsonPath("$.bodyParts[1].name").value("Biceps"))
                .andExpect(jsonPath("$.bodyParts[1].metric.delta").value(-0.5))
                .andExpect(jsonPath("$.bodyParts[1].points.length()").value(2))
                .andExpect(jsonPath("$.avgDailyStepsThisWeek").value(8100))
                .andExpect(jsonPath("$.checkinDoneThisWeek").value(true))
                .andExpect(jsonPath("$.checkinDue").value(false));

        mvc.perform(get("/api/checkins").with(asUser()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weekStart").value("2026-11-09"))
                .andExpect(jsonPath("$[1].weekStart").value("2026-10-26"))
                .andExpect(jsonPath("$[1].measurements[1].valueCm").value(29.0));
    }

    @Test
    void checkinIsDueOnSundayWhenNotDoneThisWeek() throws Exception {
        checkin("""
                {"date": "2026-11-08", "measurements": [{"partId": 1, "valueCm": 79}]}
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
                .andExpect(jsonPath("$.bodyParts[0].metric").doesNotExist())
                .andExpect(jsonPath("$.bodyParts[0].points.length()").value(0))
                .andExpect(jsonPath("$.checkinDue").value(true));
    }

    @Test
    void deletingAWeekKeepsSyncedWeight() throws Exception {
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78.5}], "weightKg": 64.2,
                 "avgDailySteps": 8100}
                """).andExpect(status().isOk());
        jdbc.sql("INSERT INTO weight_entry (user_id, measured_at, value_kg, source) VALUES (1, '2026-11-12 07:10', 64.4, 'HEALTH')")
                .update();

        mvc.perform(delete("/api/checkins/2026-11-11").with(asUser())).andExpect(status().isNoContent());

        assertThat(count("body_measurement")).isZero();
        assertThat(count("weekly_steps")).isZero();
        assertThat(jdbc.sql("SELECT source FROM weight_entry").query(String.class).list()).containsExactly("HEALTH");
    }

    @Test
    void bodyPartsCanBeAddedRenamedAndHidden() throws Exception {
        addPart("Biceps");
        mvc.perform(put("/api/body-parts/2").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Biceps prawy\"}"))
                .andExpect(jsonPath("$.name").value("Biceps prawy"));
        checkin("""
                {"date": "2026-11-15", "measurements": [{"partId": 2, "valueCm": 28}]}
                """).andExpect(status().isOk());

        mvc.perform(delete("/api/body-parts/2").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/body-parts/2").with(asUser())).andExpect(status().isNotFound());

        mvc.perform(get("/api/body-parts").with(asUser()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Talia"));
        // the hidden part's history is kept in the database but not shown
        assertThat(count("body_measurement")).isEqualTo(1);
        mvc.perform(get("/api/checkins").with(asUser())).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void movingAverageUsesTheLastSevenDays() {
        var points = List.of(
                new Point(LocalDate.parse("2026-11-01"), new BigDecimal("65")),
                new Point(LocalDate.parse("2026-11-03"), new BigDecimal("64")),
                new Point(LocalDate.parse("2026-11-10"), new BigDecimal("63")));
        var trend = CheckinService.movingAverage(points, 7);
        assertThat(trend.get(1).value()).isEqualByComparingTo("64.5");
        assertThat(trend.get(2).value()).isEqualByComparingTo("63");
    }
}
