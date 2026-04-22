package bt3.service.File;

import bt3.common.EventType;
import bt3.service.Chat.MessageEnvelope;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class Check extends Command {

    @Override
    public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {
        List<String> fileListInfo = fileService.handleCheckCommand();

        MessageEnvelope<?> response = new MessageEnvelope<>(
                EventType.LIST_FILE,
                fileListInfo,
                0,
                request.senderId()
        );

        os.writeObject(response);
        os.flush();
        os.reset();
        System.out.println("Đã gửi danh sách file cho Client " + request.senderId() + " yêu cầu.");
    }
}