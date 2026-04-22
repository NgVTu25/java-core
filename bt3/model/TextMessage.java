package bt3.model;

public record TextMessage(String content, int senderId, int receiverId) implements MessagePayload {
}
