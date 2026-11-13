package app.talia.auth.service;

import app.talia.config.TaliaProperties;
import org.springframework.stereotype.Component;

/**
 * Texts of the account e-mails in the user's language. Links carry the token in the URL fragment (#t=...),
 * which browsers never send to a server, so it does not end up in access logs.
 */
@Component
public class AccountMailService {

    private final MailService mailer;
    private final String publicUrl;
    private final TaliaProperties.Security security;

    AccountMailService(MailService mailer, TaliaProperties properties) {
        this.mailer = mailer;
        this.publicUrl = properties.publicUrl().replaceAll("/+$", "");
        this.security = properties.security();
    }

    public void verifyEmail(String to, String username, String token, String language) {
        String link = publicUrl + "/verify#t=" + token;
        long hours = security.verifyLinkTtl().toHours();
        if ("en".equals(language)) {
            mailer.send(to, "Confirm your e-mail - Talia", """
                    Hi %s,

                    welcome to Talia! Please confirm your e-mail address to activate your account:

                    %s

                    The link is valid for %d hours. If you did not create an account, just ignore this message.

                    Talia
                    """.formatted(username, link, hours));
        } else {
            mailer.send(to, "Potwierdź e-mail - Talia", """
                    Cześć %s,

                    witaj w Talii! Potwierdź swój adres e-mail, żeby aktywować konto:

                    %s

                    Link jest ważny %d godz. Jeśli to nie Ty zakładasz konto, po prostu zignoruj tę wiadomość.

                    Talia
                    """.formatted(username, link, hours));
        }
    }

    public void resetPassword(String to, String username, String token, String language) {
        String link = publicUrl + "/reset#t=" + token;
        long minutes = security.resetLinkTtl().toMinutes();
        if ("en".equals(language)) {
            mailer.send(to, "Set a new password - Talia", """
                    Hi %s,

                    to set a new password for your account "%s", open:

                    %s

                    The link is valid for %d minutes and works once. If you did not ask for it, ignore this message:
                    your password stays the same.

                    Talia
                    """.formatted(username, username, link, minutes));
        } else {
            mailer.send(to, "Ustaw nowe hasło - Talia", """
                    Cześć %s,

                    żeby ustawić nowe hasło do konta „%s”, otwórz:

                    %s

                    Link jest ważny %d min i działa jeden raz. Jeśli to nie Ty prosisz o zmianę, zignoruj tę
                    wiadomość: hasło się nie zmieni.

                    Talia
                    """.formatted(username, username, link, minutes));
        }
    }

    /** Someone tried to register with an address that already has an account. */
    public void alreadyRegistered(String to, String username, String language) {
        String link = publicUrl + "/login";
        if ("en".equals(language)) {
            mailer.send(to, "You already have an account - Talia", """
                    Hi,

                    someone (hopefully you) tried to create a Talia account with this e-mail address,
                    but it already belongs to the account "%s". Log in at %s
                    or use "Forgot password?" there.

                    If it was not you, ignore this message.

                    Talia
                    """.formatted(username, link));
        } else {
            mailer.send(to, "Masz już konto - Talia", """
                    Cześć,

                    ktoś (mamy nadzieję, że Ty) próbował założyć konto w Talii na ten adres e-mail,
                    ale należy on już do konta „%s”. Zaloguj się na %s
                    albo użyj tam opcji „Nie pamiętasz hasła?”.

                    Jeśli to nie Ty, zignoruj tę wiadomość.

                    Talia
                    """.formatted(username, link));
        }
    }
}
