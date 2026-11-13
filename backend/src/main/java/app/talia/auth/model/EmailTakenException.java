package app.talia.auth.model;

/** The e-mail address belongs to another account (the caller answers as if it worked, see AuthController). */
public class EmailTakenException extends RuntimeException {
    public EmailTakenException() {
        super("E-mail is already used");
    }
}
