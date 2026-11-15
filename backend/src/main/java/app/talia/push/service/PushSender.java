package app.talia.push.service;

import app.talia.push.entity.PushSubscriptionEntity;
import app.talia.push.model.PushMessage;
import app.talia.push.model.SendResult;

/** Delivers one message to one subscription (the Web Push HTTP request; replaced by a mock in tests). */
public interface PushSender {

    SendResult send(PushSubscriptionEntity subscription, PushMessage message);
}
