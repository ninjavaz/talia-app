package app.talia.habit;

import app.talia.IntegrationTest;
import app.talia.auth.model.TaliaUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** talia's default habits: 1 "Suplementy", 2 "Picie wody", 3 steps (see IntegrationTest). Today is Sun 15 Nov 2026. */
class HabitIntegrationTest extends IntegrationTest {

    private void check(long habitId, LocalDate day) {
        jdbc.sql("INSERT INTO habit_check (habit_id, day) VALUES (:id, :day)").param("id", habitId).param("day", day)
                .update();
    }

    private void checkAll(String day) {
        for (long id = 1; id <= 3; id++) {
            check(id, LocalDate.parse(day));
        }
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        return mvc.perform(request.with(asUser()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions stats() throws Exception {
        return mvc.perform(get("/api/habits/stats").with(asUser()));
    }

    // REQ-010 configurable list

    @Test
    @DisplayName("REQ-010 a new account starts with supplements, water and the steps habit")
    void req010DefaultHabits() throws Exception {
        mvc.perform(get("/api/habits").with(asUser()))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Suplementy"))
                .andExpect(jsonPath("$[0].kind").value("NORMAL"))
                .andExpect(jsonPath("$[0].remindAt").doesNotExist())
                .andExpect(jsonPath("$[1].name").value("Picie wody"))
                .andExpect(jsonPath("$[2].kind").value("STEPS"));

        TaliaUser ewa = accounts.create("ewa", "ewa@example.org", "Moje-Haslo-2026", "en");
        mvc.perform(get("/api/habits").with(as(ewa)))
                .andExpect(jsonPath("$[0].name").value("Supplements"))
                .andExpect(jsonPath("$[1].name").value("Drinking water"))
                .andExpect(jsonPath("$[2].kind").value("STEPS"));
    }

    @Test
    @DisplayName("REQ-010 habits can be added, edited and deleted")
    void req010AddEditDelete() throws Exception {
        json(post("/api/habits"), "{\"name\": \"  Witamina D \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Witamina D"))
                .andExpect(jsonPath("$.kind").value("NORMAL"))
                .andExpect(jsonPath("$.sortOrder").value(4));
        json(put("/api/habits/4"), "{\"name\": \"Witamina D3\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Witamina D3"));
        mvc.perform(delete("/api/habits/2").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/habits/2").with(asUser())).andExpect(status().isNotFound());

        mvc.perform(get("/api/habits").with(asUser()))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Suplementy"))
                .andExpect(jsonPath("$[1].kind").value("STEPS"))
                .andExpect(jsonPath("$[2].name").value("Witamina D3"));
    }

    @Test
    @DisplayName("REQ-010 a habit needs a name")
    void req010NameRequired() throws Exception {
        json(post("/api/habits"), "{\"name\": \"  \"}").andExpect(status().isBadRequest());
        json(post("/api/habits"), "{\"name\": \"" + "x".repeat(101) + "\"}").andExpect(status().isBadRequest());
        json(put("/api/habits/1"), "{\"name\": \"\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("REQ-010 the order can be changed and is the same everywhere")
    void req010Reorder() throws Exception {
        json(put("/api/habits/order"), "{\"ids\": [3, 1, 2]}").andExpect(status().isNoContent());

        mvc.perform(get("/api/habits").with(asUser()))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[2].id").value(2));
        mvc.perform(get("/api/habits/day").with(asUser()))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[2].id").value(2));
        // a new habit goes to the end
        json(post("/api/habits"), "{\"name\": \"Spacer\"}").andExpect(jsonPath("$.sortOrder").value(4));
    }

    @Test
    @DisplayName("REQ-010 a reorder must contain exactly the user's habits")
    void req010InvalidReorder() throws Exception {
        json(put("/api/habits/order"), "{\"ids\": [2, 1]}").andExpect(status().isBadRequest());
        json(put("/api/habits/order"), "{\"ids\": [3, 2, 1, 99]}").andExpect(status().isBadRequest());
        json(put("/api/habits/order"), "{\"ids\": [3, 3, 1]}").andExpect(status().isBadRequest());
        json(put("/api/habits/order"), "{\"ids\": []}").andExpect(status().isBadRequest());

        mvc.perform(get("/api/habits").with(asUser())).andExpect(jsonPath("$[0].id").value(1));
    }

    // REQ-011 optional time

    @Test
    @DisplayName("REQ-011 a habit can have a time, shown on the daily list, and the time can be removed")
    void req011HabitWithTime() throws Exception {
        json(post("/api/habits"), "{\"name\": \"Witamina D\", \"remindAt\": \"08:00\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remindAt").value("08:00"));
        mvc.perform(get("/api/habits/day").with(asUser()))
                .andExpect(jsonPath("$[3].name").value("Witamina D"))
                .andExpect(jsonPath("$[3].remindAt").value("08:00"));

        json(put("/api/habits/4"), "{\"name\": \"Witamina D\", \"remindAt\": null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindAt").doesNotExist());
    }

    @Test
    @DisplayName("REQ-011 an invalid time is rejected")
    void req011InvalidTime() throws Exception {
        json(post("/api/habits"), "{\"name\": \"Witamina D\", \"remindAt\": \"25:00\"}")
                .andExpect(status().isBadRequest());
        json(put("/api/habits/1"), "{\"name\": \"Suplementy\", \"remindAt\": \"rano\"}")
                .andExpect(status().isBadRequest());
    }

    // REQ-012 one-tap check

    @Test
    @DisplayName("REQ-012 checking and unchecking is idempotent")
    void req012CheckAndUncheck() throws Exception {
        mvc.perform(put("/api/habits/2/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(put("/api/habits/2/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());

        mvc.perform(get("/api/habits/day").with(asUser()))
                .andExpect(jsonPath("$[0].done").value(false))
                .andExpect(jsonPath("$[1].done").value(true));

        mvc.perform(delete("/api/habits/2/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/habits/2/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(get("/api/habits/day").with(asUser())).andExpect(jsonPath("$[1].done").value(false));
    }

    @Test
    @DisplayName("REQ-012 future days and unknown habits cannot be checked")
    void req012FutureDayAndUnknownHabit() throws Exception {
        mvc.perform(put("/api/habits/1/checks/2026-11-16").with(asUser())).andExpect(status().isBadRequest());
        mvc.perform(put("/api/habits/99/checks/2026-11-15").with(asUser())).andExpect(status().isNotFound());
    }

    // REQ-013 steps habit

    @Test
    @DisplayName("REQ-013 there is at most one steps habit")
    void req013OnlyOneStepsHabit() throws Exception {
        json(post("/api/habits"), "{\"kind\": \"STEPS\"}").andExpect(status().isConflict());

        mvc.perform(delete("/api/habits/3").with(asUser())).andExpect(status().isNoContent());
        json(post("/api/habits"), "{\"kind\": \"STEPS\", \"remindAt\": \"20:00\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("STEPS"))
                .andExpect(jsonPath("$.remindAt").value("20:00"));
    }

    @Test
    @DisplayName("REQ-013 the steps habit cannot be renamed, but its time can be set")
    void req013StepsHabitNotRenamable() throws Exception {
        json(put("/api/habits/3"), "{\"name\": \"Kroki 5000\"}").andExpect(status().isBadRequest());
        json(put("/api/habits/3"), "{\"remindAt\": \"20:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("STEPS"))
                .andExpect(jsonPath("$.remindAt").value("20:00"));
        // a normal habit cannot be created without a name, even when it says STEPS is not wanted
        json(post("/api/habits"), "{\"kind\": \"NORMAL\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("REQ-013 the steps habit is checked by hand like any habit")
    void req013StepsHabitCheckedByHand() throws Exception {
        mvc.perform(put("/api/habits/3/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(get("/api/habits/day").with(asUser()))
                .andExpect(jsonPath("$[2].kind").value("STEPS"))
                .andExpect(jsonPath("$[2].done").value(true));
    }

    // REQ-014 daily progress

    @Test
    @DisplayName("REQ-014 today's progress counts checked habits out of all habits")
    void req014DailyProgress() throws Exception {
        check(1, LocalDate.parse("2026-11-15"));
        check(3, LocalDate.parse("2026-11-15"));
        check(2, LocalDate.parse("2026-11-14"));

        stats().andExpect(jsonPath("$.doneToday").value(2)).andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/week").with(asUser()))
                .andExpect(jsonPath("$.habits.doneToday").value(2))
                .andExpect(jsonPath("$.habits.total").value(3));
    }

    // REQ-015 streak of completed days

    @Test
    @DisplayName("REQ-015 the streak counts completed days and grows when today is completed")
    void req015StreakOfCompletedDays() throws Exception {
        checkAll("2026-11-12");
        checkAll("2026-11-13");
        checkAll("2026-11-14");
        check(1, LocalDate.parse("2026-11-11"));
        check(2, LocalDate.parse("2026-11-11"));
        check(1, LocalDate.parse("2026-11-15"));

        stats().andExpect(jsonPath("$.streakDays").value(3));

        mvc.perform(put("/api/habits/2/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(put("/api/habits/3/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        stats().andExpect(jsonPath("$.streakDays").value(4));
    }

    @Test
    @DisplayName("REQ-015 a habit added today does not break the streak")
    void req015NewHabitDoesNotBreakTheStreak() throws Exception {
        checkAll("2026-11-13");
        checkAll("2026-11-14");
        checkAll("2026-11-15");
        stats().andExpect(jsonPath("$.streakDays").value(3));

        json(post("/api/habits"), "{\"name\": \"Witamina D\"}").andExpect(status().isCreated());
        // today is incomplete again (4 habits), the earlier days stay completed
        stats().andExpect(jsonPath("$.streakDays").value(2));
        mvc.perform(put("/api/habits/4/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        stats().andExpect(jsonPath("$.streakDays").value(3));
    }

    @Test
    @DisplayName("REQ-015 a deleted habit counts only on the days it was active")
    void req015DeletedHabitCountsOnlyWhileActive() throws Exception {
        checkAll("2026-11-12");
        // "Picie wody" deleted on 13 Nov: from then on only habits 1 and 3 count
        jdbc.sql("UPDATE habit SET active = false, deactivated_on = '2026-11-13' WHERE id = 2").update();
        for (String day : new String[]{"2026-11-13", "2026-11-14"}) {
            check(1, LocalDate.parse(day));
            check(3, LocalDate.parse(day));
        }

        stats().andExpect(jsonPath("$.streakDays").value(3)).andExpect(jsonPath("$.total").value(2));

        // a habit deleted today still had to be done on the days before
        mvc.perform(delete("/api/habits/3").with(asUser())).andExpect(status().isNoContent());
        stats().andExpect(jsonPath("$.streakDays").value(3));
    }

    @Test
    @DisplayName("REQ-015 a missed day breaks the streak")
    void req015MissedDayBreaksTheStreak() throws Exception {
        checkAll("2026-11-12");
        checkAll("2026-11-13");
        check(1, LocalDate.parse("2026-11-14"));
        checkAll("2026-11-15");

        stats().andExpect(jsonPath("$.streakDays").value(1));
    }
}
