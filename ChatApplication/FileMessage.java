import java.io.Serializable;

public class FileMessage implements Serializable {

    private String fileName;

    private byte[] data;

    public FileMessage(String fileName, byte[] data) {

        this.fileName = fileName;

        this.data = data;
    }

    public String getFileName() {

        return fileName;
    }

    public byte[] getData() {

        return data;
    }
}