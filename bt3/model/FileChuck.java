package bt3.model;

import java.io.Serializable;

public class FileChuck implements Serializable {
    private String fileName;
    private Long offset;
    private byte[] data;
    private String status;
    public boolean completed;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public byte[] getData() {
        return data;
    }

    public boolean getCompleted() {
        return completed;
    }


    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
