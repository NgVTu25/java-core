package bt3;

import bt3.common.EventType;

import java.io.Serial;
import java.io.Serializable;

public record MessageEnvelope(EventType type, Object payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
