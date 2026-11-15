package app.talia.push;

import app.talia.IntegrationTest;
import app.talia.push.model.PushMessage;
import app.talia.push.model.SendResult;
import app.talia.push.service.PushReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reminder rules. Now is Sunday 15 Nov 2026, 10:00 in Warsaw - the default time of the weekly check-in reminder.
 * talia has one device; habits 1 "Suplementy", 2 "Picie wody", 3 steps (see IntegrationTest).
 */
class PushReminderIntegrationTest extends IntegrationTest {

    @Autowired
    PushReminderService reminders;

    @BeforeEach
    void device() {
        jdbc.sql("""
                INSERT INTO push_subscription (user_id, endpoint, p256dh, auth)
                VALUES (1, 'https://fcm.googleapis.com/fcm/send/talia', 'key', 'auth')
                """).update();
        when(pushSender.send(any(), any())).thenReturn(SendResult.OK);
    }

    /** Messages sent so far whose tag starts with {@code tagPrefix}. */
    private List<PushMessage> sent(String tagPrefix) {
        if (mockingDetails(pushSender).getInvocations().isEmpty()) {
            return List.of();
        }
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender, atLeast(0)).send(any(), captor.capture());
        return captor.getAllValues().stream().filter(m -> m.tag().startsWith(tagPrefix)).toList();
    }

    private void habitTime(long habitId, String time) {
        jdbc.sql("UPDATE habit SET remind_at = CAST(:t AS time) WHERE id = :id").param("t", time).param("id", habitId)
                .update();
    }

    private void check(long habitId, String day) {
        jdbc.sql("INSERT INTO habit_check (habit_id, day) VALUES (:id, :day)").param("id", habitId)
                .param("day", LocalDate.parse(day)).update();
    }

    private void settings(String json) throws Exception {
        mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private static final String BASE = "\"weeklyWorkoutGoal\": 3, \"dailyStepsGoal\": 8000, \"language\": \"pl\"";

    // REQ-030

    @Test
    @DisplayName("REQ-030 a habit is reminded once at its time when not done")
    void req030HabitReminder() {
        habitTime(1, "10:00");
        reminders.sendDue();
        reminders.sendDue();

        List<PushMessage> habits = sent("habit-");
        assertThat(habits).hasSize(1);
        assertThat(habits.getFirst().body()).isEqualTo("Pora na: Suplementy");
        assertThat(habits.getFirst().tag()).isEqualTo("habit-1");
        assertThat(habits.getFirst().url()).isEqualTo("/week");
    }

    @Test
    @DisplayName("REQ-030 the steps habit is reminded with its goal")
    void req030StepsHabitReminder() {
        habitTime(3, "09:45");
        reminders.sendDue();
        assertThat(sent("habit-")).extracting(PushMessage::body).containsExactly("Pora na: Kroki: 8 000");
    }

    @Test
    @DisplayName("REQ-030 no reminder for a habit already done, not due yet, or more than 30 minutes late")
    void req030NoReminder() {
        habitTime(1, "10:00");
        check(1, "2026-11-15");
        habitTime(2, "10:05");
        habitTime(3, "09:29");
        reminders.sendDue();
        assertThat(sent("habit-")).isEmpty();
    }

    @Test
    @DisplayName("REQ-030 habit reminders can be switched off")
    void req030Disabled() throws Exception {
        settings("{" + BASE + ", \"pushHabits\": false}");
        habitTime(1, "10:00");
        reminders.sendDue();
        assertThat(sent("habit-")).isEmpty();
    }

    @Test
    @DisplayName("REQ-030 users without a device get nothing")
    void req030NoDevice() {
        jdbc.sql("DELETE FROM push_subscription").update();
        habitTime(1, "10:00");
        reminders.sendDue();
        assertThat(mockingDetails(pushSender).getInvocations()).isEmpty();
    }

    // REQ-031

    @Test
    @DisplayName("REQ-031 the weekly check-in is reminded once on Sunday 10:00 by default")
    void req031CheckinReminder() {
        reminders.sendDue();
        reminders.sendDue();

        List<PushMessage> checkins = sent("checkin");
        assertThat(checkins).hasSize(1);
        assertThat(checkins.getFirst().title()).isEqualTo("Czas na cotygodniowy pomiar");
        assertThat(checkins.getFirst().url()).isEqualTo("/progress");
    }

    @Test
    @DisplayName("REQ-031 no check-in reminder when this week's check-in is done")
    void req031CheckinDone() throws Exception {
        mvc.perform(put("/api/checkins").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\": \"2026-11-14\", \"weightKg\": 64}")).andExpect(status().isOk());
        reminders.sendDue();
        assertThat(sent("checkin")).isEmpty();
    }

    @Test
    @DisplayName("REQ-031 the reminder follows the configured day and can be switched off")
    void req031DayAndSwitch() throws Exception {
        settings("{" + BASE + ", \"checkinReminderDay\": 6, \"checkinReminderTime\": \"09:00\"}");
        reminders.sendDue();
        assertThat(sent("checkin")).isEmpty();

        settings("{" + BASE + ", \"checkinReminderDay\": 7, \"checkinReminderTime\": \"10:00\", \"pushCheckin\": false}");
        reminders.sendDue();
        assertThat(sent("checkin")).isEmpty();
    }

    // REQ-034

    @Test
    @DisplayName("REQ-034 an incomplete day is reminded at the evening time with the habits left")
    void req034StreakAtRisk() throws Exception {
        settings("{" + BASE + ", \"streakReminderTime\": \"10:00\"}");
        for (String day : new String[]{"2026-11-13", "2026-11-14"}) {
            for (long id = 1; id <= 3; id++) {
                check(id, day);
            }
        }
        check(1, "2026-11-15");
        reminders.sendDue();
        reminders.sendDue();

        List<PushMessage> streak = sent("streak");
        assertThat(streak).hasSize(1);
        assertThat(streak.getFirst().title()).isEqualTo("Nie przerwij serii (2)");
        assertThat(streak.getFirst().body()).isEqualTo("Do odhaczenia: 2 z 3.");
    }

    @Test
    @DisplayName("REQ-034 without a streak the reminder names today's habits")
    void req034NoStreakYet() throws Exception {
        settings("{" + BASE + ", \"streakReminderTime\": \"10:00\"}");
        reminders.sendDue();
        assertThat(sent("streak")).extracting(PushMessage::title).containsExactly("Nawyki na dziś");
    }

    @Test
    @DisplayName("REQ-034 no reminder when the day is completed or the reminder is off")
    void req034NoReminder() throws Exception {
        settings("{" + BASE + ", \"streakReminderTime\": \"10:00\"}");
        for (long id = 1; id <= 3; id++) {
            check(id, "2026-11-15");
        }
        reminders.sendDue();
        assertThat(sent("streak")).isEmpty();

        jdbc.sql("DELETE FROM habit_check").update();
        settings("{" + BASE + ", \"streakReminderTime\": \"10:00\", \"pushStreak\": false}");
        reminders.sendDue();
        assertThat(sent("streak")).isEmpty();
    }

    // REQ-033 settings

    @Test
    @DisplayName("REQ-033 reminder settings have defaults, can be changed and are validated")
    void req033Settings() throws Exception {
        mvc.perform(get("/api/settings").with(asUser()))
                .andExpect(jsonPath("$.pushHabits").value(true))
                .andExpect(jsonPath("$.pushCheckin").value(true))
                .andExpect(jsonPath("$.pushStreak").value(true))
                .andExpect(jsonPath("$.checkinReminderDay").value(7))
                .andExpect(jsonPath("$.checkinReminderTime").value("10:00"))
                .andExpect(jsonPath("$.streakReminderTime").value("20:00"));

        settings("{" + BASE + ", \"pushHabits\": false, \"checkinReminderDay\": 1, \"checkinReminderTime\": \"07:30\","
                + " \"streakReminderTime\": \"21:15\"}");
        // fields left out keep their values
        settings("{" + BASE + "}");
        mvc.perform(get("/api/settings").with(asUser()))
                .andExpect(jsonPath("$.pushHabits").value(false))
                .andExpect(jsonPath("$.checkinReminderDay").value(1))
                .andExpect(jsonPath("$.checkinReminderTime").value("07:30"))
                .andExpect(jsonPath("$.streakReminderTime").value("21:15"));

        for (String invalid : new String[]{"\"checkinReminderDay\": 8", "\"checkinReminderDay\": 0",
                "\"checkinReminderTime\": \"25:00\"", "\"streakReminderTime\": \"wieczorem\""}) {
            mvc.perform(put("/api/settings").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                    .content("{" + BASE + ", " + invalid + "}")).andExpect(status().isBadRequest());
        }
    }
}
