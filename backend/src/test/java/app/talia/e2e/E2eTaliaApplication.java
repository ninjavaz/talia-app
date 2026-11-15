package app.talia.e2e;

import app.talia.TaliaApplication;
import app.talia.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

import java.util.Arrays;

/**
 * Backend for the Playwright e2e tests (frontend/e2e): a throwaway Postgres container, port 8081 and the
 * test-only {@link E2eSupport} endpoints. Lives in the test sources, so none of it ships in the application jar.
 * Started by playwright.config.ts via {@code mvnw spring-boot:test-run -Dspring-boot.run.main-class=...}.
 */
public class E2eTaliaApplication {

    public static void main(String[] args) {
        String[] withProfile = Arrays.copyOf(args, args.length + 1);
        withProfile[args.length] = "--spring.profiles.active=test,e2e";
        SpringApplication.from(TaliaApplication::main)
                .with(TestcontainersConfiguration.class, E2eSupport.class)
                .run(withProfile);
    }
}
