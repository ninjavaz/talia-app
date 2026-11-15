package app.talia.push.config;

import app.talia.config.TaliaProperties;
import app.talia.push.service.Vapid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The server's VAPID identity: configured keys, or a random pair per start when none are configured (dev). */
@Configuration(proxyBeanMethods = false)
class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    Vapid vapid(TaliaProperties properties) {
        TaliaProperties.Push push = properties.push();
        if (isBlank(push.vapidPublicKey()) || isBlank(push.vapidPrivateKey())) {
            log.warn("No VAPID keys configured (VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY): using a random pair for this "
                    + "run. Devices re-subscribe after a restart. Set the keys in production.");
            return Vapid.generate(push.vapidSubject());
        }
        return Vapid.of(push.vapidPublicKey(), push.vapidPrivateKey(), push.vapidSubject());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
