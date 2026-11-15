package app.talia.push.model;

/** Outcome of one push request: delivered to the push service, subscription gone (delete it), or failed. */
public enum SendResult {
    OK, GONE, FAILED
}
