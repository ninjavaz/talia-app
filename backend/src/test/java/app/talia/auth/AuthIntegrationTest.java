package app.talia.auth;

import app.talia.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    private ResultActions login(String username, String password, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    /** Fetches a challenge and solves it by brute force, like the browser does. */
    private String solvedProof(String ip) throws Exception {
        String json = mvc.perform(get("/api/auth/challenge").with(fromIp(ip)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String salt = JsonPath.read(json, "$.salt");
        String challenge = JsonPath.read(json, "$.challenge");
        int max = JsonPath.read(json, "$.maxNumber");
        for (int n = 0; n <= max; n++) {
            if (ProofOfWork.sha256(salt + n).equals(challenge)) {
                return """
                        {"algorithm":"SHA-256","challenge":"%s","salt":"%s","number":%d,"signature":"%s"}
                        """.formatted(challenge, salt, n, JsonPath.read(json, "$.signature"));
            }
        }
        throw new AssertionError("unsolvable challenge");
    }

    private ResultActions register(String username, String password, String proof, String website, String ip)
            throws Exception {
        return mvc.perform(post("/api/auth/register").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","proof":%s,"website":"%s"}
                        """.formatted(username, password, proof, website)));
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
    }

    @Test
    void registrationCreatesAnAccountAndLogsIn() throws Exception {
        String ip = "10.3.0.1";
        MockHttpSession session = (MockHttpSession) register("Ola.K", "Moje-Haslo-2026", solvedProof(ip), "", ip)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ola.k"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.username").value("ola.k"));
        // a fresh account has default settings and the built-in waist, but no data
        mvc.perform(get("/api/body-parts").session(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("WAIST"));
        mvc.perform(get("/api/habits").session(session)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/settings").session(session)).andExpect(jsonPath("$.weeklyWorkoutGoal").value(3));
        String hash = jdbc.sql("SELECT password_hash FROM app_user WHERE username = 'ola.k'").query(String.class).single();
        assertThat(hash).startsWith("{bcrypt}$2a$12$");
    }

    @Test
    void registrationRejectsBotsAndBadInput() throws Exception {
        String ip = "10.4.0.1";
        // missing / forged proof of work
        register("bot1", "Moje-Haslo-2026", """
                {"algorithm":"SHA-256","challenge":"abc","salt":"x?expires=9999999999","number":1,"signature":"abc"}
                """, "", ip).andExpect(status().isBadRequest());
        // honeypot field filled in
        register("bot2", "Moje-Haslo-2026", solvedProof(ip), "http://spam.example", ip)
                .andExpect(status().isBadRequest());
        // the third registration attempt from this IP is still allowed (limit 3 in tests): weak password
        register("ola", "1234567890", solvedProof(ip), "", ip).andExpect(status().isBadRequest());
        // fourth attempt within the hour from the same IP
        register("ola", "Moje-Haslo-2026", solvedProof(ip), "", ip).andExpect(status().isTooManyRequests());
        assertThat(jdbc.sql("SELECT count(*) FROM app_user").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void solvedChallengeCannotBeReused() throws Exception {
        String ip = "10.5.0.1";
        String proof = solvedProof(ip);
        register("ola", "Moje-Haslo-2026", proof, "", ip).andExpect(status().isCreated());
        register("ola2", "Moje-Haslo-2026", proof, "", ip).andExpect(status().isBadRequest());
    }

    @Test
    void usernameMustBeUniqueAndValid() throws Exception {
        register("TALIA", "Moje-Haslo-2026", solvedProof("10.6.0.1"), "", "10.6.0.1")
                .andExpect(status().isConflict());
        register("a b", "Moje-Haslo-2026", solvedProof("10.6.0.2"), "", "10.6.0.2")
                .andExpect(status().isBadRequest());
        register("ola", "ola-ola-ola-1", solvedProof("10.6.0.3"), "", "10.6.0.3")
                .andExpect(status().isBadRequest()); // contains the username
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
        assertThat(stored).isEqualTo(ProofOfWork.sha256(token)).isNotEqualTo(token);

        mvc.perform(post("/api/health-sync").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        // the old token stopped working
        mvc.perform(post("/api/health-sync").header("Authorization", "Bearer " + SYNC_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
