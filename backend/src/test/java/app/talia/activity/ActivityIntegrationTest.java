package app.talia.activity;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

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
    void twoWorkoutsOfTheSameTypeOnOneDayAreBothKept() throws Exception {
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"WALK\", \"durationMin\": 30}");
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"WALK\", \"durationMin\": 30}")
                .andExpect(jsonPath("$.startedAt").value("2026-11-12T12:01:00"));
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutsDone").value(2));
    }

    @Test
    void deleteWorkout() throws Exception {
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 45}");
        mvc.perform(delete("/api/workouts/1").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/workouts/1").with(asUser())).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidWorkouts() throws Exception {
        addWorkout("{\"date\": \"2026-11-16\", \"category\": \"GYM\", \"durationMin\": 45}")
                .andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"YOGA\", \"durationMin\": 45}")
                .andExpect(status().isBadRequest());
        addWorkout("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 0}")
                .andExpect(status().isBadRequest());
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
    void settingsCanBeChanged() throws Exception {
        mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weeklyWorkoutGoal": 4, "dailyStepsGoal": 10000, "habitWeekThresholdPct": 80,
                                 "language": "en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyWorkoutGoal").value(4));
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutGoal").value(4));

        mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weeklyWorkoutGoal": 99, "dailyStepsGoal": 10000, "habitWeekThresholdPct": 80,
                                 "language": "de"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
