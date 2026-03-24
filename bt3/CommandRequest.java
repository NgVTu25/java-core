package bt3;

import java.io.Serial;
import java.io.Serializable;

public record CommandRequest(String commandType, String filename, long offset) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}