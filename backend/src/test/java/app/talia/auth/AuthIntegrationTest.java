package app.talia.auth;

import app.talia.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"talia\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMeLogout() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"talia\",\"password\":\"talia\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("talia"));

        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
