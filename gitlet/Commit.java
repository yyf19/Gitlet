package gitlet;
import static gitlet.Utils.*;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;



/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author yyf
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The reference of this Commit. */
    private String SHA1;
    /** The reference of the parent of this Commit. */
    private String parentSHA1;
    /** The reference of the second parent for merge Commit. */
    private String secondParentSHA1;
    /** The message of this Commit. */
    private String message;
    /** The time of this Commit. */
    private Date time;
    /** The tracked files of this Commit. Blobs maps filenames (key) to their SHA1s (value). */
    public HashMap<String, String> Blobs;

    /*
    for initial commit, no Blobs and parent.
     */
    public Commit(){
        time = new Date(0);
        message = "initial commit";
        Blobs = new HashMap<>();
        SHA1 = Utils.sha1(message, time.toString());
        parentSHA1 = null;
        secondParentSHA1 = null;
    }

    /*
    create a commit, exit here (failure case) doesn't work, should be in repo.java
     */
    public Commit(String message){
        String currCommSHA1 = readContentsAsString(Utils.join(Repository.BRANCH_DIR, readContentsAsString(join(Repository.BRANCH_DIR, "head"))));
        Commit parentCommit = Commit.fromFile(currCommSHA1);
        time = new java.util.Date();
        this.message = message;
        /** make a copy of its parent's Blobs */
        Blobs = parentCommit.Blobs;

        /*
        track all files in stagingArea/toTrack folder
         */
        File stagingarea = join(".gitlet", "stagingArea", "toTrack");
        if(stagingarea.exists()){
            File[] allFiles = stagingarea.listFiles();
            for(File f:allFiles){
                Blobs.put(f.getName(), Utils.sha1(readContents(f)));
                File fDirec = join(Repository.BLOBS_DIR, f.getName());
                if(!fDirec.exists()){
                    fDirec.mkdir(); //creates a folder with the name of f
                }
                writeContents(join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))), readContents(f));
            }
        }

         /*
        remove all files in stagingArea/toRemove folder
         */
        File removeFile = join(".gitlet", "stagingArea", "toRemove");
        if(removeFile.exists()){
            File[] allFiles = removeFile.listFiles();
            for(File f:allFiles){
                Blobs.remove(f.getName());
                /* ????????????????????????????????????????????????????only remove from commit.Blob, keep saved file in blobs directory
                if(join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))).exists()) {
                    join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))).delete();
                }

                 */
            }
        }
        /*
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!not sure sha1 calculation is correct
         */
        SHA1 = Utils.sha1(message, time.toString());
        parentSHA1 = parentCommit.SHA1;
        secondParentSHA1 = null;

        Repository.clearStagingArea();
    }

    public Commit(String message, String currSHA1, String givenSHA1){
        Commit parentCommit = Commit.fromFile(currSHA1);
        time = new java.util.Date();
        this.message = message;
        /** make a copy of its parent's Blobs */
        Blobs = parentCommit.Blobs;

        /*
        track all files in stagingArea/toTrack folder
         */
        File stagingarea = join(".gitlet", "stagingArea", "toTrack");
        if(stagingarea.exists()){
            File[] allFiles = stagingarea.listFiles();
            for(File f:allFiles){
                Blobs.put(f.getName(), Utils.sha1(readContents(f)));
                File fDirec = join(Repository.BLOBS_DIR, f.getName());
                if(!fDirec.exists()){
                    fDirec.mkdir(); //creates a folder with the name of f
                }
                writeContents(join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))), readContents(f));
            }
        }

         /*
        remove all files in stagingArea/toRemove folder
         */
        File removeFile = join(".gitlet", "stagingArea", "toRemove");
        if(removeFile.exists()){
            File[] allFiles = removeFile.listFiles();
            for(File f:allFiles){
                Blobs.remove(f.getName());
                /* ????????????????????????????????????????????????????only remove from commit.Blob, keep saved file in blobs directory
                if(join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))).exists()) {
                    join(Repository.BLOBS_DIR, f.getName(), Utils.sha1(readContents(f))).delete();
                }

                 */
            }
        }
        /*
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!not sure sha1 calculation is correct
         */
        SHA1 = Utils.sha1(message, time.toString());
        parentSHA1 = currSHA1;
        secondParentSHA1 = givenSHA1;

        /*
        clear the staging area
         */
        String[] entries = stagingarea.list();
        if(entries != null) {
            for (String s : entries) {
                File currentFile = new File(stagingarea.getPath(), s);
                currentFile.delete();
            }
            stagingarea.delete();
        }

        String[] entriesRemove = removeFile.list();
        if(entriesRemove != null) {
            for (String s : entriesRemove) {
                File currentFile = new File(removeFile.getPath(), s);
                currentFile.delete();
            }
            removeFile.delete();
        }
        join(".gitlet", "stagingArea").delete();
    }

    /*
    write this commit to the file named as SHA1 in commits folder
    */
    public void saveCommit(){
        File COMMIT_FOLDER = Utils.join(Repository.COMMITS_DIR, SHA1);
        writeObject(COMMIT_FOLDER, this);
    }

    public String SHA1(){
        return SHA1;
    }

    public Date time(){
        return time;
    }

    public String message(){
        return message;
    }

    public String parentSHA1(){
        return parentSHA1;
    }

    public String secondParentSHA1(){
        return  secondParentSHA1;}


    /*
    return the Commit with ref. of sha1
     */
    public static Commit fromFile(String sha1) {

        File f = Utils.join(Repository.COMMITS_DIR, sha1);
        if(!f.exists()){
            /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            print may need to be deleted
             */
            System.out.println("no such commit.");
            return null;
        }
        return Utils.readObject(f, Commit.class);
    }

}
