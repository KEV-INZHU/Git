package gitlet;

import java.util.HashMap;
import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;
import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 * does at a high level.
 *
 * @author KEVIN ZHU
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**
     * The message of this Commit.
     */
    private String message;
    /**
     * The parent of this Commit.
     */
    private String parent;
    /**
     * The time of this Commit.
     */
    private String time;

    private HashMap<String, String> tracked;
    private String id;
    private String mergeParent;

    public Commit(String msg, String par, HashMap<String, String> track) {
        Date now = new java.util.Date();
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM d hh:mm:ss YYYY");
        time = format.format(now) + " -0800";
        message = msg;
        parent = par;
        tracked = track;
        id = setId();
        mergeParent = null;
    }

    public String setId() {
        byte[] commit = serialize(this);
        return sha1(commit);
    }

    public String initialTimeSet() {
        time = "Thu Jan 1 00:00:00 1970 -0800";
        return time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public HashMap<String, String> getTracked() {
        return tracked;
    }

    public void setTracked(HashMap<String, String> tracked) {
        this.tracked = tracked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMergeParent() {
        return mergeParent;
    }

    public void setMergeParent(String mergeParent) {
        this.mergeParent = mergeParent;
    }
}
