package bt3.model;

public record PrivateChatMessage(int senderId, String content) implements MessagePayload {
}