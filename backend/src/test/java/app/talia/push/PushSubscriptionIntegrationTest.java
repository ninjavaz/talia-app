package app.talia.push;

import app.talia.IntegrationTest;
import app.talia.auth.model.TaliaUser;
import app.talia.push.model.SendResult;
import app.talia.push.service.EcKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REQ-033: push subscriptions of a user's devices. */
class PushSubscriptionIntegrationTest extends IntegrationTest {

    private static final String FCM = "https://fcm.googleapis.com/fcm/send/device-";

    private static String deviceKey() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(EcKeys.raw(EcKeys.generate()));
    }

    private static String json(String endpoint, String p256dh, String auth) {
        return """
                {"endpoint": "%s", "keys": {"p256dh": "%s", "auth": "%s"}}""".formatted(endpoint, p256dh, auth);
    }

    private static String valid(String endpoint) {
        return json(endpoint, deviceKey(), "BTBZMqHH6r4Tts7J_aSIgg");
    }

    private ResultActions subscribe(TaliaUser user, String body) throws Exception {
        return mvc.perform(put("/api/push/subscriptions").with(as(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int count() {
        return jdbc.sql("SELECT count(*) FROM push_subscription").query(Integer.class).single();
    }

    @Test
    @DisplayName("REQ-033 push needs a login, the server key is public to logged-in users")
    void keyAndLogin() throws Exception {
        mvc.perform(get("/api/push/key")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON).content(valid(FCM + "1")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/push/key").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(org.hamcrest.Matchers.hasLength(87)));
    }

    @Test
    @DisplayName("REQ-033 a device subscribes, subscribes again and unsubscribes")
    void subscribeAndUnsubscribe() throws Exception {
        subscribe(talia, valid(FCM + "1")).andExpect(status().isNoContent());
        subscribe(talia, valid(FCM + "1")).andExpect(status().isNoContent());
        assertThat(count()).isEqualTo(1);

        mvc.perform(delete("/api/push/subscriptions").with(asUser()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\": \"" + FCM + "1\"}")).andExpect(status().isNoContent());
        assertThat(count()).isZero();
    }

    @Test
    @DisplayName("REQ-033 only HTTPS endpoints of the known push services are accepted")
    void unknownEndpointsRejected() throws Exception {
        for (String endpoint : new String[]{
                "http://fcm.googleapis.com/fcm/send/x",
                "https://evil.example.com/push",
                "https://fcm.googleapis.com.evil.example.com/push",
                "https://notfcm.googleapis.com/push",
                "https://localhost/push",
                "https://127.0.0.1/push",
                "ftp://fcm.googleapis.com/x",
                "not a url"}) {
            subscribe(talia, valid(endpoint)).andExpect(status().isBadRequest());
        }
        // the push services that are allowed
        subscribe(talia, valid("https://updates.push.services.mozilla.com/wpush/v2/x")).andExpect(status().isNoContent());
        subscribe(talia, valid("https://web.push.apple.com/QH1x")).andExpect(status().isNoContent());
        subscribe(talia, valid("https://wns2-par02p.notify.windows.com/w/?token=x")).andExpect(status().isNoContent());
        assertThat(count()).isEqualTo(3);
    }

    @Test
    @DisplayName("REQ-033 invalid device keys are rejected")
    void invalidKeysRejected() throws Exception {
        subscribe(talia, json(FCM + "1", "abc", "BTBZMqHH6r4Tts7J_aSIgg")).andExpect(status().isBadRequest());
        // a 65-byte value that is not a point on the curve
        String notOnCurve = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[65]);
        subscribe(talia, json(FCM + "1", notOnCurve, "BTBZMqHH6r4Tts7J_aSIgg")).andExpect(status().isBadRequest());
        subscribe(talia, json(FCM + "1", deviceKey(), "c2hvcnQ")).andExpect(status().isBadRequest());
        assertThat(count()).isZero();
    }

    @Test
    @DisplayName("REQ-033 a user keeps at most 10 devices, the oldest is dropped")
    void subscriptionLimit() throws Exception {
        for (int i = 1; i <= 11; i++) {
            subscribe(talia, valid(FCM + i)).andExpect(status().isNoContent());
        }
        assertThat(count()).isEqualTo(10);
        assertThat(jdbc.sql("SELECT count(*) FROM push_subscription WHERE endpoint = :e").param("e", FCM + "1")
                .query(Integer.class).single()).isZero();
    }

    @Test
    @DisplayName("REQ-033 a device belongs to the account that subscribed last; others cannot remove it")
    void deviceMovesBetweenAccounts() throws Exception {
        TaliaUser ola = accounts.create("ola", "Inne-Haslo-2026");
        subscribe(talia, valid(FCM + "1")).andExpect(status().isNoContent());

        mvc.perform(delete("/api/push/subscriptions").with(as(ola)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\": \"" + FCM + "1\"}")).andExpect(status().isNoContent());
        assertThat(count()).isEqualTo(1);

        subscribe(ola, valid(FCM + "1")).andExpect(status().isNoContent());
        assertThat(jdbc.sql("SELECT user_id FROM push_subscription").query(Long.class).single()).isEqualTo(ola.id());
    }

    @Test
    @DisplayName("REQ-033 a test notification goes to the user's devices; gone devices are removed")
    void testNotification() throws Exception {
        subscribe(talia, valid(FCM + "1")).andExpect(status().isNoContent());
        when(pushSender.send(any(), any())).thenReturn(SendResult.OK);
        mvc.perform(post("/api/push/test").with(asUser())).andExpect(jsonPath("$.sent").value(1));

        when(pushSender.send(any(), any())).thenReturn(SendResult.GONE);
        mvc.perform(post("/api/push/test").with(asUser())).andExpect(jsonPath("$.sent").value(0));
        assertThat(count()).isZero();
    }
}
