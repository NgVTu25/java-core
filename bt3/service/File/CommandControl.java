package bt3.service.File;

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
        if (requestHeader.equals(EventType.CLIENT_LIST.name())) {
            return new ClientList();
        }
        return null;
    }
}
