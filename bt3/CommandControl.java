package bt3;

public class CommandControl {

    public static Command getCommand(String requestHeader) {
        if (requestHeader.equalsIgnoreCase("DOWNLOAD")) {
            return new Download();
        }
        if (requestHeader.equalsIgnoreCase("UPLOAD")) {
            return new Upload();
        }
        if (requestHeader.equalsIgnoreCase("CHECK")) {
            return new Check();
        }
        return null;
    }
}
