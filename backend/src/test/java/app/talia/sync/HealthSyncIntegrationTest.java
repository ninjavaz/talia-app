package app.talia.sync;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthSyncIntegrationTest extends IntegrationTest {

    private static final String TOKEN = "test-token";

    private static final String FULL_PAYLOAD = """
            {
              "steps": [
                {"date": "2026-11-06", "value": 9120},
                {"date": "2026-11-07", "value": 6400},
                {"date": "2026-11-08", "value": 7432}
              ],
              "weight": [
                {"ts": "2026-11-07T07:05:00", "value": 68.6},
                {"ts": "2026-11-08T07:12:00", "value": 68.4}
              ],
              "workouts": [
                {"start": "2026-11-06T18:00:00", "end": "2026-11-06T19:05:00",
                 "type": "Traditional Strength Training", "kcal": 310},
                {"start": "2026-11-08T10:00:00", "end": "2026-11-08T10:50:00", "type": "Pilates", "kcal": 180}
              ]
            }
            """;

    private ResultActions sync(String json, String token) throws Exception {
        var request = post("/api/health-sync").contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    @Test
    void rejectsMissingToken() throws Exception {
        sync(FULL_PAYLOAD, null).andExpect(status().isUnauthorized());
        assertThat(count("daily_steps")).isZero();
    }

    @Test
    void rejectsWrongToken() throws Exception {
        sync(FULL_PAYLOAD, "wrong-token").andExpect(status().isUnauthorized());
        assertThat(count("daily_steps")).isZero();
    }

    @Test
    void sessionUserIsForbiddenOnSync() throws Exception {
        mvc.perform(post("/api/health-sync")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("talia").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstSyncInsertsEverything() throws Exception {
        sync(FULL_PAYLOAD, TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.inserted").value(3))
                .andExpect(jsonPath("$.weight.inserted").value(2))
                .andExpect(jsonPath("$.workouts.inserted").value(2))
                .andExpect(jsonPath("$.steps.updated").value(0));

        assertThat(count("daily_steps")).isEqualTo(3);
        assertThat(count("weight_entry")).isEqualTo(2);
        assertThat(count("workout")).isEqualTo(2);
        assertThat(count("sync_log")).isEqualTo(1);
    }

    @Test
    void repeatedSyncIsHarmless() throws Exception {
        sync(FULL_PAYLOAD, TOKEN).andExpect(status().isOk());

        sync(FULL_PAYLOAD, TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.inserted").value(0))
                .andExpect(jsonPath("$.steps.updated").value(0))
                .andExpect(jsonPath("$.steps.unchanged").value(3))
                .andExpect(jsonPath("$.weight.unchanged").value(2))
                .andExpect(jsonPath("$.workouts.unchanged").value(2));

        assertThat(count("daily_steps")).isEqualTo(3);
        assertThat(count("weight_entry")).isEqualTo(2);
        assertThat(count("workout")).isEqualTo(2);
    }

    @Test
    void changedValuesAreUpdated() throws Exception {
        sync(FULL_PAYLOAD, TOKEN).andExpect(status().isOk());

        sync("""
                {"steps": [{"date": "2026-11-08", "value": 10250}, {"date": "2026-11-09", "value": 1200}],
                 "workouts": [{"start": "2026-11-08T10:00:00", "end": "2026-11-08T11:00:00",
                               "type": "Pilates", "kcal": 220}]}
                """, TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.inserted").value(1))
                .andExpect(jsonPath("$.steps.updated").value(1))
                .andExpect(jsonPath("$.workouts.updated").value(1))
                .andExpect(jsonPath("$.weight.inserted").value(0));

        Integer steps = jdbc.sql("SELECT steps FROM daily_steps WHERE day = DATE '2026-11-08'")
                .query(Integer.class).single();
        assertThat(steps).isEqualTo(10250);
        assertThat(count("workout")).isEqualTo(2);
    }

    @Test
    void sameStartDifferentTypeIsAnotherWorkout() throws Exception {
        sync("""
                {"workouts": [
                  {"start": "2026-11-08T10:00:00", "end": "2026-11-08T10:30:00", "type": "Walking"},
                  {"start": "2026-11-08T10:00:00", "end": "2026-11-08T10:30:00", "type": "Pilates"}
                ]}
                """, TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workouts.inserted").value(2));
    }

    @Test
    void workoutCategoryIsDerivedFromType() throws Exception {
        sync(FULL_PAYLOAD, TOKEN).andExpect(status().isOk());

        assertThat(jdbc.sql("SELECT category FROM workout ORDER BY started_at").query(String.class).list())
                .containsExactly("GYM", "PILATES");
    }

    @Test
    void timestampsWithOffsetAreConvertedToLocalTime() throws Exception {
        sync("""
                {"weight": [{"ts": "2026-11-08T06:12:00Z", "value": 68.4},
                            {"ts": "2026-11-09 07:30:00+01:00", "value": 68.2}]}
                """, TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight.inserted").value(2));

        assertThat(jdbc.sql("SELECT measured_at FROM weight_entry ORDER BY measured_at")
                .query(LocalDateTime.class).list())
                .containsExactly(LocalDateTime.parse("2026-11-08T07:12:00"), LocalDateTime.parse("2026-11-09T07:30:00"));
    }

    @Test
    void emptyPayloadIsAccepted() throws Exception {
        sync("{}", TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.inserted").value(0));
    }

    @Test
    void invalidValuesAreRejectedWithoutPartialWrites() throws Exception {
        sync("""
                {"steps": [{"date": "2026-11-08", "value": 7000}],
                 "weight": [{"ts": "2026-11-08T07:12:00", "value": 5}]}
                """, TOKEN)
                .andExpect(status().isBadRequest());

        sync("""
                {"steps": [{"date": "2026-11-08", "value": 7000}],
                 "weight": [{"ts": "yesterday morning", "value": 68.4}]}
                """, TOKEN)
                .andExpect(status().isBadRequest());

        assertThat(count("daily_steps")).isZero();
        assertThat(count("weight_entry")).isZero();
    }

    @Test
    void weightIsStoredAsHealthSource() throws Exception {
        sync(FULL_PAYLOAD, TOKEN).andExpect(status().isOk());

        var row = jdbc.sql("SELECT value_kg, source FROM weight_entry WHERE measured_at = TIMESTAMP '2026-11-08 07:12:00'")
                .query((rs, n) -> new Object[]{rs.getBigDecimal(1), rs.getString(2)}).single();
        assertThat((BigDecimal) row[0]).isEqualByComparingTo("68.4");
        assertThat(row[1]).isEqualTo("HEALTH");
    }
}
