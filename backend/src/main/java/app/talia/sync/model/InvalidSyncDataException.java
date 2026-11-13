package app.talia.sync.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSyncDataException extends RuntimeException {

    public InvalidSyncDataException(String message) {
        super(message);
    }
}
