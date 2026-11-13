package app.talia.auth.model;

/** Right password, but the e-mail is not confirmed yet (a fresh link was sent). */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("E-mail not confirmed");
    }
}
