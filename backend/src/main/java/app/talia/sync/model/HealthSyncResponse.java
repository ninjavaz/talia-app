package app.talia.sync.model;

public record HealthSyncResponse(Counts steps, Counts weight, Counts workouts) {

    public record Counts(int inserted, int updated, int unchanged) {
    }
}
