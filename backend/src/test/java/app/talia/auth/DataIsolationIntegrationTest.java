package app.talia.auth;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One account must never see or change another account's data, even when it guesses ids. */
class DataIsolationIntegrationTest extends IntegrationTest {

    private TaliaUser ola;

    @BeforeEach
    void talliaHasDataOlaHasNone() throws Exception {
        ola = accounts.create("ola", "Inne-Haslo-2026");
        mvc.perform(put("/api/checkins").with(asUser()).contentType(MediaType.APPLICATION_JSON).content("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 78}], "weightKg": 64, "avgDailySteps": 8000}
                """)).andExpect(status().isOk());
        mvc.perform(post("/api/workouts").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\": \"2026-11-12\", \"category\": \"GYM\", \"durationMin\": 60}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/habits/1/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(post("/api/health-sync").header("Authorization", "Bearer " + SYNC_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"steps\": [{\"date\": \"2026-11-14\", \"value\": 9000}]}")).andExpect(status().isOk());
    }

    @Test
    void readsOnlyReturnOwnData() throws Exception {
        mvc.perform(get("/api/progress").with(as(ola)))
                .andExpect(jsonPath("$.waist").doesNotExist())
                .andExpect(jsonPath("$.weightPoints.length()").value(0))
                .andExpect(jsonPath("$.bodyParts.length()").value(1))
                .andExpect(jsonPath("$.avgDailyStepsThisWeek").doesNotExist());
        mvc.perform(get("/api/checkins").with(as(ola))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/week").with(as(ola)))
                .andExpect(jsonPath("$.workoutsDone").value(0))
                .andExpect(jsonPath("$.habits.total").value(0));
        mvc.perform(get("/api/habits").with(as(ola))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/workouts?from=2026-11-01&to=2026-11-30").with(as(ola)))
                .andExpect(jsonPath("$.length()").value(0));

        // and talia still sees hers
        mvc.perform(get("/api/week").with(asUser())).andExpect(jsonPath("$.workoutsDone").value(1));
    }

    @Test
    void foreignIdsBehaveLikeMissingOnes() throws Exception {
        mvc.perform(delete("/api/workouts/1").with(as(ola))).andExpect(status().isNotFound());
        mvc.perform(put("/api/habits/1/checks/2026-11-14").with(as(ola))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/habits/1").with(as(ola))).andExpect(status().isNotFound());
        mvc.perform(put("/api/habits/1").with(as(ola)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"hacked\"}")).andExpect(status().isNotFound());
        mvc.perform(put("/api/body-parts/1").with(as(ola)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"hacked\"}")).andExpect(status().isNotFound());
        // talia's waist is body part 1; ola cannot write measurements into it
        mvc.perform(put("/api/checkins").with(as(ola)).contentType(MediaType.APPLICATION_JSON).content("""
                {"date": "2026-11-15", "measurements": [{"partId": 1, "valueCm": 99}]}
                """)).andExpect(status().isBadRequest());
        // deleting "her" week does not touch talia's check-in
        mvc.perform(delete("/api/checkins/2026-11-15").with(as(ola))).andExpect(status().isNoContent());

        mvc.perform(get("/api/checkins").with(asUser()))
                .andExpect(jsonPath("$[0].measurements[0].valueCm").value(78.0))
                .andExpect(jsonPath("$[0].weightKg").value(64.0));
        mvc.perform(get("/api/habits").with(asUser())).andExpect(jsonPath("$[0].name").value("Białko w posiłkach"));
    }

    @Test
    void syncTokenWritesOnlyIntoItsOwnersAccount() throws Exception {
        mvc.perform(get("/api/week?date=2026-11-14").with(as(ola)))
                .andExpect(jsonPath("$.avgDailySteps").doesNotExist());
    }
}
