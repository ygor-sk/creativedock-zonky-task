package sk.ygor.creativedock.zonky;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RestApiControllerException extends RuntimeException {

    public RestApiControllerException(String message) {
        super(message);
    }

}
