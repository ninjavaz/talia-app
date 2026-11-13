package app.talia;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Base class: full Spring context against a real Postgres (shared container via the cached context). */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected JdbcClient jdbc;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvcAndCleanData() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.sql("TRUNCATE daily_steps, weight_entry, waist_entry, workout, sync_log").update();
    }
}
