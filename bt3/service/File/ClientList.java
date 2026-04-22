package bt3.service.File;

import bt3.Server.Server;
import bt3.common.EventType;
import bt3.service.Chat.MessageEnvelope;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ClientList extends Command {

	@Override
	public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {
		List<Integer> ids = new ArrayList<>(Server.getClientIds());

		MessageEnvelope<?> response = new MessageEnvelope<>(
				EventType.CLIENT_LIST,
				ids,
				0,
				request.senderId()
		);

		os.writeObject(response);
		os.flush();
		os.reset();

		System.out.println("Đã gửi danh sách Client ID cho Client " + request.senderId());
	}
}