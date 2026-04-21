package bt3;

import bt3.common.EventType;

public class CommandControl {

    public static Command getCommand(String requestHeader) {
        if (requestHeader.equals(EventType.FILE_DOWNLOAD.name())) {
            return new Download();
        }
        if (requestHeader.equals(EventType.FILE_UPLOAD.name())) {
            return new Upload();
        }
        if (requestHeader.equals(EventType.LIST_FILE.name())) {
            return new Check();
        }
        return null;
    }
}
