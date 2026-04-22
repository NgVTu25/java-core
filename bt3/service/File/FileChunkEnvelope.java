package bt3.service.File;

import bt3.common.EventType;
import bt3.model.FileChuck;
import bt3.service.Chat.MessageEnvelope;

public class FileChunkEnvelope extends MessageEnvelope<FileChuck> {

    public FileChunkEnvelope(EventType type, FileChuck payload, int senderId, int receiverId) {
        super(type, payload, senderId, receiverId);
    }
}