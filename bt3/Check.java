package bt3;

import bt3.Server.FileService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class Check extends Command {

    @Override
    public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {
        List<String> fileListInfo = fileService.handleCheckCommand();
        os.writeObject(fileListInfo);
        os.flush();
        os.reset();
        System.out.println("Đã gửi danh sách file cho Client yêu cầu.");
    }
}
