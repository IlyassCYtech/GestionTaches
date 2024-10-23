import java.sql.Timestamp;

public class Task {
    private String text;
    private String priority;
    private boolean done;
    private Timestamp date;

    public Task(String text, String priority, boolean done, Timestamp date) {
        this.text = text;
        this.priority = priority;
        this.done = done;
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isDone() {
        return done;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
