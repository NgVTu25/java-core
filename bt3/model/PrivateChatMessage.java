package bt3.model;

public record PrivateChatMessage(int receiverId, String content) implements MessagePayload {
}