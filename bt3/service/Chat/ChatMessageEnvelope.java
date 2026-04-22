package bt3.service.Chat;

import bt3.common.EventType;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

public class ChatMessageEnvelope extends MessageEnvelope<TextMessage> {

    public ChatMessageEnvelope(TextMessage payload, int senderId) {
        super(EventType.CHAT, payload, senderId, -1);
    }

    public ChatMessageEnvelope(PrivateChatMessage payload, int senderId) {
        super(EventType.PRIVATE_CHAT, new TextMessage(payload.content(), senderId, payload.receiverId()), senderId, payload.receiverId());
    }
}