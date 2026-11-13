package app.talia.auth.model;

public enum RateLimit {
    /** failed logins from one IP */
    LOGIN_IP,
    /** failed logins for one username, from any IP */
    LOGIN_USER,
    REGISTER_IP,
    CHALLENGE_IP,
    /** account e-mails requested from one IP */
    MAIL_IP,
    /** account e-mails sent to one address */
    MAIL_ADDRESS
}
