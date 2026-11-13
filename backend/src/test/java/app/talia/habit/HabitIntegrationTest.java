package app.talia.habit;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HabitIntegrationTest extends IntegrationTest {

    private void check(long habitId, LocalDate day) {
        jdbc.sql("INSERT INTO habit_check (habit_id, day) VALUES (:id, :day)").param("id", habitId).param("day", day)
                .update();
    }

    @Test
    void createRenameAndDelete() throws Exception {
        mvc.perform(post("/api/habits").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  8 000 kroków \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("8 000 kroków"))
                .andExpect(jsonPath("$.sortOrder").value(4));

        mvc.perform(put("/api/habits/4").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"10 000 kroków\"}"))
                .andExpect(jsonPath("$.name").value("10 000 kroków"));

        mvc.perform(delete("/api/habits/1").with(asUser())).andExpect(status().isNoContent());

        mvc.perform(get("/api/habits").with(asUser()))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Warzywa do 2 posiłków"));
    }

    @Test
    void checkAndUncheckIsIdempotent() throws Exception {
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
    void cannotCheckFutureDaysOrUnknownHabits() throws Exception {
        mvc.perform(put("/api/habits/1/checks/2026-11-16").with(asUser())).andExpect(status().isBadRequest());
        mvc.perform(put("/api/habits/99/checks/2026-11-15").with(asUser())).andExpect(status().isNotFound());
    }

    @Test
    void streakCountsWeeksMeetingTheThreshold() throws Exception {
        // last week (2-8 Nov) and the week before (26 Oct - 1 Nov): all 3 habits on 6 of 7 days = 86% >= 70%
        for (LocalDate monday : new LocalDate[]{LocalDate.parse("2026-11-02"), LocalDate.parse("2026-10-26")}) {
            for (int d = 0; d < 6; d++) {
                for (long h = 1; h <= 3; h++) {
                    check(h, monday.plusDays(d));
                }
            }
        }
        // three weeks ago: only 1 check - breaks the streak
        check(1, LocalDate.parse("2026-10-20"));
        // this week (9-15 Nov, today Sunday): 2 of 3 habits every day = 67% < 70% -> running week not counted yet
        for (int d = 0; d < 7; d++) {
            check(1, LocalDate.parse("2026-11-09").plusDays(d));
            check(2, LocalDate.parse("2026-11-09").plusDays(d));
        }

        mvc.perform(get("/api/habits/stats").with(asUser()))
                .andExpect(jsonPath("$.doneToday").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.weekPct").value(67))
                .andExpect(jsonPath("$.thresholdPct").value(70))
                .andExpect(jsonPath("$.streakWeeks").value(2));

        // one more check today pushes this week over the threshold: 15/21 = 71%
        mvc.perform(put("/api/habits/3/checks/2026-11-15").with(asUser())).andExpect(status().isNoContent());
        mvc.perform(get("/api/habits/stats").with(asUser()))
                .andExpect(jsonPath("$.weekPct").value(71))
                .andExpect(jsonPath("$.streakWeeks").value(3));
    }
}
