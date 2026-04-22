package bt3.service.File;

import bt3.common.EventType;
import java.io.Serializable;

public record CommandRequest(EventType commandType, String filename, long offset, int senderId) implements Serializable {
}