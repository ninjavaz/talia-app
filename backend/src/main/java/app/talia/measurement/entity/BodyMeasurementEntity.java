package app.talia.measurement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Table {@code body_measurement}: one value (cm) of a body part per day. */
@Entity
@Table(name = "body_measurement")
public class BodyMeasurementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long bodyPartId;
    private LocalDate measuredOn;
    private BigDecimal valueCm;

    protected BodyMeasurementEntity() {
    }

    public BodyMeasurementEntity(long bodyPartId, LocalDate measuredOn, BigDecimal valueCm) {
        this.bodyPartId = bodyPartId;
        this.measuredOn = measuredOn;
        this.valueCm = valueCm;
    }

    public Long getId() {
        return id;
    }

    public long getBodyPartId() {
        return bodyPartId;
    }

    public LocalDate getMeasuredOn() {
        return measuredOn;
    }

    public BigDecimal getValueCm() {
        return valueCm;
    }
}
