package app.talia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    /** All "today" / "this week" logic uses this clock, so it follows the user's timezone, not the server's. */
    @Bean
    Clock clock(TaliaProperties properties) {
        return Clock.system(properties.timezone());
    }
}
