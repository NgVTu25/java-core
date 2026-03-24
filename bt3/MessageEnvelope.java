package bt3;

import bt3.Server.FileService;
import bt3.model.Messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Message implements Command {
    private Messages message;

    @Override
    public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {



    }

}
