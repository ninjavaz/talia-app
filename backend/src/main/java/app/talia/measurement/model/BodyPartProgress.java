package app.talia.measurement.model;

import java.util.List;

public record BodyPartProgress(long id, String name, String code, Metric metric, List<Point> points) {
}
