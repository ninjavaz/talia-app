package app.talia.auth.model;

/** Account data needed by the e-mail flows. */
public record Account(long id, String username, String email, String language, boolean emailVerified) {
}
