package app.talia.auth;

/** A rate limit was hit; answered with 429 and a Retry-After header (see AuthExceptionHandler). */
class TooManyRequestsException extends RuntimeException {

    final long retryAfterSeconds;

    TooManyRequestsException(long retryAfterSeconds) {
        super("Too many attempts");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
