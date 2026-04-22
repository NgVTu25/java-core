package bt3.service.Chat;

import bt3.common.EventType;
import java.io.Serializable;

public class MessageEnvelope<T> implements Serializable {
    protected final EventType type;
    protected final T payload;
    protected int senderId;
    protected int receiverId;

    public MessageEnvelope(EventType type, T payload, int senderId, int receiverId) {
        this.type = type;
        this.payload = payload;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public EventType getType() { return type; }
    public T getPayload() { return payload; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
}