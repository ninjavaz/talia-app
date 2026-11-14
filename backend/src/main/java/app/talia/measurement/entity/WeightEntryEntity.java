package app.talia.measurement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Table {@code weight_entry}: one weight per user and timestamp, from the check-in ({@code MANUAL}) or synced from
 * Apple Health ({@code HEALTH}). Written by upserts, see the repository.
 */
@Entity
@Table(name = "weight_entry")
public class WeightEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;
    private LocalDateTime measuredAt;
    private BigDecimal valueKg;
    private String source;

    protected WeightEntryEntity() {
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public BigDecimal getValueKg() {
        return valueKg;
    }

    public String getSource() {
        return source;
    }
}
