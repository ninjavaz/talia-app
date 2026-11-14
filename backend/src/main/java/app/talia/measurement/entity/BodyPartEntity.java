package app.talia.measurement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table {@code body_part}: what the user measures (waist, hips, biceps...). Deleting only deactivates it. */
@Entity
@Table(name = "body_part")
public class BodyPartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private String name;
    private int sortOrder;
    private boolean active = true;

    protected BodyPartEntity() {
    }

    public BodyPartEntity(long userId, String name, int sortOrder) {
        this.userId = userId;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}
