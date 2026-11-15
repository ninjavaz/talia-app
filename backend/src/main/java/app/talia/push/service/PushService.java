package app.talia.push.service;

import app.talia.common.BadRequestException;
import app.talia.config.TaliaProperties;
import app.talia.push.entity.PushSubscriptionEntity;
import app.talia.push.model.PushKey;
import app.talia.push.model.PushMessage;
import app.talia.push.model.PushSubscriptionRequest;
import app.talia.push.model.SendResult;
import app.talia.push.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Base64;
import java.util.List;

/**
 * Push subscriptions of the users' devices and sending to all devices of a user (REQ-033). Sending happens outside
 * of a transaction (it waits for the push services).
 */
@Service
public class PushService {

    private final PushSubscriptionRepository subscriptions;
    private final PushSender sender;
    private final Vapid vapid;
    private final TaliaProperties.Push push;
    private final Clock clock;

    PushService(PushSubscriptionRepository subscriptions, PushSender sender, Vapid vapid, TaliaProperties properties,
                Clock clock) {
        this.subscriptions = subscriptions;
        this.sender = sender;
        this.vapid = vapid;
        this.push = properties.push();
        this.clock = clock;
    }

    public PushKey key() {
        return new PushKey(vapid.publicKey());
    }

    /** Stores (or takes over) the device's subscription; keeps at most the configured number of devices per user. */
    @Transactional
    public void subscribe(long userId, PushSubscriptionRequest request) {
        if (!PushEndpoints.isAllowed(request.endpoint(), push)) {
            throw new BadRequestException("Not a supported push service");
        }
        validateKeys(request.keys());
        subscriptions.findByEndpoint(request.endpoint()).ifPresentOrElse(
                existing -> existing.renew(userId, request.keys().p256dh(), request.keys().auth()),
                () -> subscriptions.save(new PushSubscriptionEntity(userId, request.endpoint(),
                        request.keys().p256dh(), request.keys().auth(), clock.instant())));
        subscriptions.flush();

        List<PushSubscriptionEntity> devices = subscriptions.findByUserIdOrderByCreatedAtAscIdAsc(userId);
        for (int i = 0; i < devices.size() - push.maxSubscriptionsPerUser(); i++) {
            subscriptions.delete(devices.get(i));
        }
    }

    /** Removes this device's subscription of the user (a foreign endpoint is ignored). */
    public void unsubscribe(long userId, String endpoint) {
        subscriptions.deleteByUserAndEndpoint(userId, endpoint);
    }

    /** Sends to every device of the user; returns how many push services accepted it. */
    public int sendToUser(long userId, PushMessage message) {
        int delivered = 0;
        for (PushSubscriptionEntity subscription : subscriptions.findByUserIdOrderByCreatedAtAscIdAsc(userId)) {
            SendResult result = sender.send(subscription, message);
            if (result == SendResult.OK) {
                subscriptions.markUsed(subscription.getId(), clock.instant());
                delivered++;
            } else if (result == SendResult.GONE) {
                subscriptions.deleteById(subscription.getId());
            }
        }
        return delivered;
    }

    public List<Long> usersWithDevices() {
        return subscriptions.findUserIds();
    }

    private static void validateKeys(PushSubscriptionRequest.Keys keys) {
        try {
            Base64.Decoder b64 = Base64.getUrlDecoder();
            EcKeys.publicKey(b64.decode(keys.p256dh()));
            if (b64.decode(keys.auth()).length != 16) {
                throw new BadRequestException("Invalid push auth secret");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid push subscription keys");
        }
    }
}
