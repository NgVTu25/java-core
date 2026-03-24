package bt3;

import bt3.Server.FileService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Command {
    void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException;
}
