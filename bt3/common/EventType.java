package bt3.common;

import java.io.Serializable;

public enum EventType implements Serializable {

    CHAT,

    PRIVATE_CHAT,

    CLIENT_LIST,

    FILE_UPLOAD,

    FILE_DOWNLOAD,

    ACCEPT,

    REJECT,

    STOP,

    SERVER_FULL,

    LIST_FILE,

    FILE_CHUNK,

    UNKNOWN
}