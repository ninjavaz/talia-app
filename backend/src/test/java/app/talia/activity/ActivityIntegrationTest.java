package app.talia.activity;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityIntegrationTest extends IntegrationTest {

    private ResultActions addWorkout(String json) throws Exception {
        return mvc.perform(post("/api/workouts").with(asUser()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("REQ-020 REQ-021 added workouts count towards the weekly goal")
    void addedWorkoutsCountTowardsTheWeeklyGoal() throws Exception {
        addWorkout("{\"date\": \"2026-11-10\", \"category\": \"GYM\", \"durationMin\": 60}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startedAt").value("2026-11-10T12:00:00"))
                .andExpect(jsonPath("$.durationMin").value(60));
        addWorkout("{\"date\": \"2026-11-15\", \"category\": \"PILATES\", \"durationMin\": 50}")
                .andExpect(jsonPath("$.startedAt").value("2026-11-15T10:00:00"));
        // last week - not counted
        addWorkout("{\"date\": \"2026-11-08\", \"category\": \"WALK\", \"durationMin\": 70}");

        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-11-09"))
                .andExpect(jsonPath("$.weekEnd").value("2026-11-15"))
                .andExpect(jsonPath("$.workoutGoal").value(3))
                .andExpect(jsonPath("$.workoutsDone").value(2))
                .andExpect(jsonPath("$.workouts[0].category").value("GYM"))
                .andExpect(jsonPath("$.habits.total").value(3));
    }

    @Test
    @DisplayName("REQ-020 two workouts of the same type on one day are both kept")
    void twoWorkoutsOfTheSameTypeOnOneDayAreBothKept() throws Exception {
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"WALK\", \"durationMin\": 30}");
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"WALK\", \"durationMin\": 30}")
                .andExpect(jsonPath("$.startedAt").value("2026-11-12T12:01:00"));
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutsDone").value(2));
    }

    @Test
    @DisplayName("REQ-020 a workout can be deleted")
    void deleteWorkout() throws Exception {
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 45}");
        mvc.perform(delete("/api/workouts/1").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/workouts/1").with(asUser())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("REQ-020 invalid workouts are rejected")
    void rejectsInvalidWorkouts() throws Exception {
        addWorkout("{\"date\": \"2026-11-16\", \"category\": \"GYM\", \"durationMin\": 45}")
                .andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"YOGA\", \"durationMin\": 45}")
                .andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 0}")
                .andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\"}").andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 601}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutsDone").value(0));
    }

    @Test
    void weeklyStepsComeFromCheckinOrSyncedDays() throws Exception {
        jdbc.sql("INSERT INTO daily_steps (user_id, day, steps) VALUES (1, '2026-11-10', 6000), (1, '2026-11-11', 8000)").update();
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.avgDailySteps").value(7000));

        mvc.perform(put("/api/checkins").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\": \"2026-11-15\", \"avgDailySteps\": 9100}"));
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.avgDailySteps").value(9100));

        mvc.perform(get("/api/week?date=2026-11-02").with(asUser()))
                .andExpect(jsonPath("$.avgDailySteps").doesNotExist());
    }

    @Test
    @DisplayName("REQ-021 the weekly goal can be changed, invalid values are rejected")
    void settingsCanBeChanged() throws Exception {
        mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weeklyWorkoutGoal": 4, "dailyStepsGoal": 10000, "language": "en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyWorkoutGoal").value(4));
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutGoal").value(4));

        mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weeklyWorkoutGoal": 99, "dailyStepsGoal": 10000, "language": "de"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private void weight(String measuredAt, String kg) {
        jdbc.sql("INSERT INTO weight_entry (user_id, measured_at, value_kg, source) "
                        + "VALUES (1, CAST(:at AS timestamp), :kg, 'MANUAL')")
                .param("at", measuredAt).param("kg", new BigDecimal(kg)).update();
    }

    @Test
    @DisplayName("REQ-021 walks count towards the weekly goal")
    void req021WalksCount() throws Exception {
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"WALK\", \"durationMin\": 40}");
        addWorkout("{\"date\": \"2026-11-13\", \"category\": \"GYM\", \"durationMin\": 40}");
        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.workoutsDone").value(2))
                .andExpect(jsonPath("$.workoutGoal").value(3));
    }

    @Test
    @DisplayName("REQ-022 kcal are estimated from the latest weight on or before the workout day")
    void req022EstimateFromLatestWeight() throws Exception {
        weight("2026-11-08 08:00", "64");
        weight("2026-11-12 08:00", "70");
        addWorkout("{\"date\": \"2026-11-10\", \"category\": \"GYM\", \"durationMin\": 60}");
        addWorkout("{\"date\": \"2026-11-15\", \"category\": \"PILATES\", \"durationMin\": 50}");

        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.workouts[0].kcal").value(320))
                .andExpect(jsonPath("$.workouts[0].kcalEstimated").value(true))
                .andExpect(jsonPath("$.workouts[1].kcal").value(175))
                .andExpect(jsonPath("$.kcalTotal").value(495));
    }

    @Test
    @DisplayName("REQ-022 a weight entered after the workout is used when there is none before")
    void req022WeightAfterTheWorkout() throws Exception {
        addWorkout("{\"date\": \"2026-11-10\", \"category\": \"PILATES\", \"durationMin\": 50}");
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workouts[0].kcal").doesNotExist());

        weight("2026-11-12 08:00", "70");
        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.workouts[0].kcal").value(175))
                .andExpect(jsonPath("$.kcalTotal").value(175));
    }

    @Test
    @DisplayName("REQ-022 without any weight there is no estimate and no weekly sum")
    void req022NoWeight() throws Exception {
        addWorkout("{\"date\": \"2026-11-10\", \"category\": \"GYM\", \"durationMin\": 60}")
                .andExpect(jsonPath("$.kcal").doesNotExist());
        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.workouts[0].kcal").doesNotExist())
                .andExpect(jsonPath("$.kcalTotal").doesNotExist());
    }

    @Test
    @DisplayName("REQ-022 kcal synced from Apple Health win over the estimate")
    void req022SyncedKcalWins() throws Exception {
        weight("2026-11-08 08:00", "64");
        jdbc.sql("""
                INSERT INTO workout (user_id, started_at, ended_at, type, category, kcal) VALUES
                    (1, '2026-11-11 18:00', '2026-11-11 19:00', 'Functional Strength Training', 'GYM', 250.4),
                    (1, '2026-11-12 18:00', '2026-11-12 19:00', 'Pilates', 'PILATES', 0)
                """).update();

        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.workouts[0].kcal").value(250))
                .andExpect(jsonPath("$.workouts[0].kcalEstimated").value(false))
                // 0 from Health means "unknown": estimated instead (3.0 x 64 x 1 h)
                .andExpect(jsonPath("$.workouts[1].kcal").value(192))
                .andExpect(jsonPath("$.workouts[1].kcalEstimated").value(true))
                .andExpect(jsonPath("$.kcalTotal").value(442));
    }
}
