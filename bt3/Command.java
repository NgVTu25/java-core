package bt3;

import bt3.Server.FileService;
import bt3.common.EventType;
import bt3.model.TextMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

abstract public class Command {
    static final Map<EventType, Command> processorMap = new HashMap<>();

    static {
        processorMap.put(EventType.FILE_DOWNLOAD, new Download());
        processorMap.put(EventType.FILE_UPLOAD, new Upload());
        processorMap.put(EventType.LIST_FILE, new Check());
    }

    public void processMessage(TextMessage message) {
        Command command = processorMap.get(message);
        if (command != null) {
            command.processMessage(message);
        } else {
            System.out.printf("Unknown command: %s%n", message.content());
        }
    }


    public abstract void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException;
}
