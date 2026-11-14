package app.talia.sync.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table {@code sync_log}: one row per Apple Health sync (for the "no sync for 48 h" alert). */
@Entity
@Table(name = "sync_log")
public class SyncLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private int inserted;
    private int updated;

    protected SyncLogEntity() {
    }

    public SyncLogEntity(long userId, int inserted, int updated) {
        this.userId = userId;
        this.inserted = inserted;
        this.updated = updated;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public int getInserted() {
        return inserted;
    }

    public int getUpdated() {
        return updated;
    }
}
