package bt3;

import bt3.common.EventType;

import java.io.Serial;
import java.io.Serializable;

public record CommandRequest(EventType commandType, String filename, long offset) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}