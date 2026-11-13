package app.talia.auth;

import app.talia.IntegrationTest;
import app.talia.auth.service.AccountCleanupService;
import app.talia.common.Hashes;
import com.jayway.jsonpath.JsonPath;
import org.altcha.altcha.v2.Altcha;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    private static final Pattern LINK_TOKEN = Pattern.compile("https://talia\\.test/(verify|reset)#t=([A-Za-z0-9_-]{43})");

    @Autowired
    AccountCleanupService cleanup;

    private ResultActions login(String username, String password, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    /** Fetches a challenge and solves it with the ALTCHA library, like the widget does in the browser. */
    private String solvedAltcha(String ip) throws Exception {
        String challengeJson = mvc.perform(get("/api/auth/challenge").with(fromIp(ip)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        Altcha.Challenge challenge = Altcha.parsePayload(base64("""
                {"challenge":%s,"solution":{"counter":0,"derivedKey":"00"}}""".formatted(challengeJson))).challenge();
        Altcha.Solution solution = Altcha.solveChallenge(challenge, Altcha.kdf(challenge.parameters().algorithm()));
        return base64("""
                {"challenge":%s,"solution":{"counter":%d,"derivedKey":"%s"}}"""
                .formatted(challengeJson, solution.counter(), solution.derivedKey()));
    }

    private static String base64(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions register(String username, String email, String password, String altcha, String website,
                                   String ip) throws Exception {
        return mvc.perform(post("/api/auth/register").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"%s","altcha":"%s","website":"%s","language":"en"}
                        """.formatted(username, email, password, altcha, website)));
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** All e-mails sent so far to the address: [subject, text] pairs. */
    private List<String[]> mailsTo(String address) {
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailer, org.mockito.Mockito.atLeast(0)).send(to.capture(), subject.capture(), text.capture());
        List<String[]> result = new java.util.ArrayList<>();
        for (int i = 0; i < to.getAllValues().size(); i++) {
            if (to.getAllValues().get(i).equals(address)) {
                result.add(new String[]{subject.getAllValues().get(i), text.getAllValues().get(i)});
            }
        }
        return result;
    }

    /** Token from the link in the latest e-mail to the address. */
    private String lastLinkToken(String address) {
        List<String[]> mails = mailsTo(address);
        assertThat(mails).isNotEmpty();
        Matcher m = LINK_TOKEN.matcher(mails.getLast()[1]);
        assertThat(m.find()).as("link in: " + mails.getLast()[1]).isTrue();
        return m.group(2);
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        login("talia", "nope-nope-nope", "10.1.0.1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        login("nobody", "nope-nope-nope", "10.1.0.1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void loginIsCaseInsensitiveMeRefreshesTheCookieAndLogoutEndsTheSession() throws Exception {
        MockHttpSession session = (MockHttpSession) login(" Talia ", PASSWORD, "10.1.0.2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("talia"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("talia"))
                .andExpect(jsonPath("$.email").value("talia@example.com"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("TALIA_SESSION="),
                        org.hamcrest.Matchers.containsString("Max-Age=31536000"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))));

        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedWrongPasswordsLockTheAccountTemporarily() throws Exception {
        accounts.create("ola", "Inne-Haslo-2026");
        for (int i = 0; i < 8; i++) {
            login("ola", "wrong-password-" + i, "10.2.0." + i).andExpect(status().isUnauthorized());
        }
        // even the right password is refused now, from any IP, with a Retry-After hint
        login("ola", "Inne-Haslo-2026", "10.2.0.99")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // other accounts are not affected
        login("talia", PASSWORD, "10.2.0.100").andExpect(status().isOk());
        // the counters live in Postgres (survive restarts) under hashed ids: no usernames or IPs in clear
        List<String> ids = jdbc.sql("SELECT id FROM rate_limit_bucket").query(String.class).list();
        assertThat(ids).isNotEmpty().allMatch(id -> id.matches("[0-9a-f]{64}"));
    }

    @Test
    void registrationNeedsTheEmailConfirmedBeforeTheFirstLogin() throws Exception {
        String ip = "10.3.0.1";
        register("Ola.K", " Ola@Example.org ", "Moje-Haslo-2026", solvedAltcha(ip), "", ip)
                .andExpect(status().isAccepted());
        String verifyToken = lastLinkToken("ola@example.org");
        assertThat(mailsTo("ola@example.org").getFirst()[0]).isEqualTo("Confirm your e-mail - Talia");

        // the right password, but not confirmed yet: no session, and a fresh link is sent
        login("ola.k", "Moje-Haslo-2026", ip)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        String newToken = lastLinkToken("ola@example.org");
        assertThat(newToken).isNotEqualTo(verifyToken);
        // the wrong password does not reveal anything about the account
        login("ola.k", "Zle-Haslo-2026", ip).andExpect(status().isUnauthorized());

        // only the newest link works, and only once
        postJson("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(verifyToken)).andExpect(status().isBadRequest());
        postJson("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(newToken)).andExpect(status().isNoContent());
        postJson("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(newToken)).andExpect(status().isBadRequest());

        MockHttpSession session = (MockHttpSession) login("ola.k", "Moje-Haslo-2026", ip)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ola@example.org"))
                .andReturn().getRequest().getSession(false);
        // a fresh account has default settings, the language from registration and the built-in waist, but no data
        mvc.perform(get("/api/body-parts").session(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("WAIST"));
        mvc.perform(get("/api/habits").session(session)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/settings").session(session))
                .andExpect(jsonPath("$.weeklyWorkoutGoal").value(3))
                .andExpect(jsonPath("$.language").value("en"));
        String hash = jdbc.sql("SELECT password_hash FROM app_user WHERE username = 'ola.k'").query(String.class).single();
        assertThat(hash).startsWith("{bcrypt}$2a$12$");
        // used links are deleted right away
        assertThat(jdbc.sql("SELECT count(*) FROM email_token").query(Integer.class).single()).isZero();
    }

    @Test
    void registeringWithAnAddressThatHasAnAccountLooksTheSameButOnlyNotifiesTheOwner() throws Exception {
        String ip = "10.3.0.2";
        register("ktos", "TALIA@example.com", "Moje-Haslo-2026", solvedAltcha(ip), "", ip)
                .andExpect(status().isAccepted());
        assertThat(jdbc.sql("SELECT count(*) FROM app_user").query(Integer.class).single()).isEqualTo(1);
        List<String[]> mails = mailsTo("talia@example.com");
        assertThat(mails).hasSize(1);
        assertThat(mails.getFirst()[0]).isEqualTo("Masz już konto - Talia");
        assertThat(mails.getFirst()[1]).contains("„talia”").doesNotContain("#t=");
    }

    @Test
    void registrationRejectsBotsAndBadInput() throws Exception {
        String ip = "10.4.0.1";
        // forged proof of work
        register("bot1", "bot1@example.org", "Moje-Haslo-2026", base64("""
                {"challenge":{"parameters":{"algorithm":"PBKDF2/SHA-256","nonce":"00","salt":"00","cost":1,"keyLength":32,"keyPrefix":"00"},"signature":"00"},"solution":{"counter":1,"derivedKey":"00"}}
                """), "", ip).andExpect(status().isBadRequest());
        // honeypot field filled in
        register("bot2", "bot2@example.org", "Moje-Haslo-2026", solvedAltcha(ip), "http://spam.example", ip)
                .andExpect(status().isBadRequest());
        // the third registration attempt from this IP is still allowed (limit 3 in tests): weak password
        register("ola", "ola@example.org", "1234567890", solvedAltcha(ip), "", ip).andExpect(status().isBadRequest());
        // fourth attempt within the hour from the same IP
        register("ola", "ola@example.org", "Moje-Haslo-2026", solvedAltcha(ip), "", ip)
                .andExpect(status().isTooManyRequests());
        assertThat(jdbc.sql("SELECT count(*) FROM app_user").query(Integer.class).single()).isEqualTo(1);
        verify(mailer, never()).send(eq("ola@example.org"), anyString(), anyString());
    }

    @Test
    void solvedChallengeCannotBeReused() throws Exception {
        String ip = "10.5.0.1";
        String altcha = solvedAltcha(ip);
        register("ola", "ola@example.org", "Moje-Haslo-2026", altcha, "", ip).andExpect(status().isAccepted());
        register("ola2", "ola2@example.org", "Moje-Haslo-2026", altcha, "", ip).andExpect(status().isBadRequest());
    }

    @Test
    void usernameMustBeUniqueAndInputValid() throws Exception {
        register("TALIA", "new@example.org", "Moje-Haslo-2026", solvedAltcha("10.6.0.1"), "", "10.6.0.1")
                .andExpect(status().isConflict());
        register("a b", "new@example.org", "Moje-Haslo-2026", solvedAltcha("10.6.0.2"), "", "10.6.0.2")
                .andExpect(status().isBadRequest());
        register("ola", "new@example.org", "ola-ola-ola-1", solvedAltcha("10.6.0.3"), "", "10.6.0.3")
                .andExpect(status().isBadRequest()); // contains the username
        register("ola", "not-an-email", "Moje-Haslo-2026", solvedAltcha("10.6.0.4"), "", "10.6.0.4")
                .andExpect(status().isBadRequest());
        register("ola", "a@b.c\\r\\nBcc: x@evil.example", "Moje-Haslo-2026", solvedAltcha("10.6.0.5"), "", "10.6.0.5")
                .andExpect(status().isBadRequest()); // no header injection
    }

    @Test
    void forgottenPasswordIsResetByEmailAndLogsOutEverywhere() throws Exception {
        // a stored session of "talia" on another device (MockMvc keeps its own sessions in memory)
        jdbc.sql("""
                INSERT INTO spring_session (primary_id, session_id, creation_time, last_access_time,
                                            max_inactive_interval, expiry_time, principal_name)
                VALUES ('00000000-0000-0000-0000-000000000001', 'other-device', 0, 0, 31536000, 9999999999999, 'talia')
                """).update();

        // unknown address: same answer, no e-mail
        postJson("/api/auth/forgot-password", "{\"email\":\"nobody@example.org\"}").andExpect(status().isAccepted());
        verify(mailer, never()).send(eq("nobody@example.org"), anyString(), anyString());

        postJson("/api/auth/forgot-password", "{\"email\":\"Talia@Example.com\"}").andExpect(status().isAccepted());
        String token = lastLinkToken("talia@example.com");
        assertThat(mailsTo("talia@example.com").getLast()[1]).contains("„talia”");

        // a weak password is refused and the link keeps working
        postJson("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"1234567890\"}".formatted(token))
                .andExpect(status().isBadRequest());
        postJson("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"Nowe-Haslo-2026\"}".formatted(token))
                .andExpect(status().isNoContent());
        // the link works once
        postJson("/api/auth/reset-password", "{\"token\":\"%s\",\"password\":\"Inne-Haslo-2027\"}".formatted(token))
                .andExpect(status().isBadRequest());

        // logged out on every device
        assertThat(jdbc.sql("SELECT count(*) FROM spring_session WHERE principal_name = 'talia'")
                .query(Integer.class).single()).isZero();
        login("talia", PASSWORD, "10.7.0.1").andExpect(status().isUnauthorized());
        login("talia", "Nowe-Haslo-2026", "10.7.0.1").andExpect(status().isOk());
    }

    @Test
    void passwordResetEmailsAreLimitedPerAddress() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/forgot-password").with(fromIp("10.8.0." + i))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"talia@example.com\"}"))
                    .andExpect(status().isAccepted());
        }
        // mails-per-address: 3 per hour, the answer stays the same
        verify(mailer, times(3)).send(eq("talia@example.com"), anyString(), anyString());
    }

    @Test
    void unconfirmedAccountsAreDeletedAfterTheLinkExpires() throws Exception {
        accounts.create("ola", "ola@example.org", "Moje-Haslo-2026", "pl");
        accounts.create("ewa", "ewa@example.org", "Moje-Haslo-2026", "pl");
        jdbc.sql("UPDATE app_user SET created_at = created_at - interval '49 hours' WHERE username = 'ola'").update();

        cleanup.run();

        assertThat(jdbc.sql("SELECT username FROM app_user ORDER BY id").query(String.class).list())
                .containsExactly("talia", "ewa");
    }

    @Test
    void personalSyncTokenIsShownOnceAndOnlyItsHashIsStored() throws Exception {
        mvc.perform(get("/api/auth/sync-token").with(asUser())).andExpect(jsonPath("$.configured").value(true));
        String json = mvc.perform(post("/api/auth/sync-token").with(asUser()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.token");
        assertThat(token).hasSizeGreaterThanOrEqualTo(40);
        String stored = jdbc.sql("SELECT sync_token_hash FROM app_user WHERE id = :id")
                .param("id", talia.id()).query(String.class).single();
        assertThat(stored).isEqualTo(Hashes.sha256(token)).isNotEqualTo(token);

        mvc.perform(post("/api/health-sync").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        // the old token stopped working
        mvc.perform(post("/api/health-sync").header("Authorization", "Bearer " + SYNC_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
