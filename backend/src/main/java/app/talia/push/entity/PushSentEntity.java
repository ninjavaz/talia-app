package app.talia.push.entity;

import app.talia.push.model.ReminderKind;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/** Table {@code push_sent}: a reminder that was sent (de-duplication). */
@Entity
@Table(name = "push_sent")
@IdClass(PushSentEntity.Key.class)
public class PushSentEntity {

    public record Key(Long userId, ReminderKind kind, Long refId, LocalDate day) implements Serializable {
    }

    @Id
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    private ReminderKind kind;

    @Id
    private Long refId;

    @Id
    private LocalDate day;

    private Instant sentAt;

    protected PushSentEntity() {
    }
}
