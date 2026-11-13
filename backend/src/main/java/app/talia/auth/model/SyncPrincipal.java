package app.talia.auth.model;

/** Principal of a request authenticated with a personal Apple Health sync token. */
public record SyncPrincipal(long userId) {
}
