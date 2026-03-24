package bt3.model;

public record TextMessage(String content, int sender, int receiver) implements MessagePayload {
}
