package app.talia.sync;

record HealthSyncResponse(Counts steps, Counts weight, Counts workouts) {

    record Counts(int inserted, int updated, int unchanged) {
    }
}
