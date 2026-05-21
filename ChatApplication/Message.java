public class Message {

    private String sender;
    private String text;

    public Message(String sender, String text) {

        this.sender = sender;
        this.text = text;
    }

    public String format() {

        return sender + ": " + text;
    }
}