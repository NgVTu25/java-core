package bt3;

import java.io.Serializable;

public class CommandRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String commandType;
    private String filename;
    private long offset;

    public CommandRequest(String commandType, String filename, long offset) {
        this.commandType = commandType;
        this.filename = filename;
        this.offset = offset;
    }

    public String getCommandType() { return commandType; }
    public String getFilename() { return filename; }
    public long getOffset() { return offset; }
}