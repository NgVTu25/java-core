package bt3;

import bt3.model.MessagePayload;

import java.io.Serial;
import java.io.Serializable;

public record MessageEnvelope(String type, MessagePayload payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
