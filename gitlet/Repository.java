package gitlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


import static gitlet.Utils.*;

import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


/** Represents a gitlet repository.
 *  The structure of a Capers Repository is as follows:
 *
 *  .gitlet/ -- top level folder for all data generated by gitlet
 *     - blobs/ -- folder containing all of the tracked files
 *     - branch -- folder containing all the branches
 *     - commits -- foler containing all the commits
 *
 *  @author yyf
 */
public class Repository {
    /**

     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */


    static String master;
    static String head;
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The commits directory. */
    public static final File COMMITS_DIR = join(".gitlet", "commits");
    /** The blobs directory. */
    public static final File BLOBS_DIR = join(".gitlet", "blobs");
    /** The branch directory, including HEAD */
    public static final File BRANCH_DIR = join(".gitlet", "branch");

    /** Create folders in .gitlet */
    public static void setupPersistence() {
        if(!GITLET_DIR.exists()){
            GITLET_DIR.mkdir();
        }
        if(!COMMITS_DIR.exists()){
            COMMITS_DIR.mkdir();
        }
        if(!BLOBS_DIR.exists()){
            BLOBS_DIR.mkdir();
        }
        if(!BRANCH_DIR.exists()){
            BRANCH_DIR.mkdir();
        }
    }

    public static void makeInitialCommit(){
        Commit iniComm = new Commit();
        iniComm.saveCommit();
        master = iniComm.SHA1();
        head = "master";
        File head_FILE = Utils.join(BRANCH_DIR, "head");
        writeContents(head_FILE, head); //write head to the file named as "head" in branch folder
        saveHead(iniComm.SHA1()); //write the ref. (SHA1) of the head of the active branch to the file named as head ("master" for this case)

    }

    public static void makeCommit(String message){
        File[] allFiles = join(".gitlet", "stagingArea", "toTrack").listFiles();
        File[] allFilesRemove = join(".gitlet", "stagingArea", "toRemove").listFiles();

        /** If no files have been staged, abort.*/
        if(allFiles == null || allFiles.length == 0){
            if(allFilesRemove == null || allFilesRemove.length == 0) {
                System.out.println("No changes added to the commit.");
                System.exit(0);
            }
        }
        Commit commit = new Commit(message);
        commit.saveCommit();
        saveHead(commit.SHA1());
    }

    public static void makeMergeCommit(String message, String currSHA1, String givenSHA1){
        Commit mergeCommit = new Commit(message, currSHA1, givenSHA1);
        mergeCommit.saveCommit();
        saveHead(mergeCommit.SHA1());
    }

    /*
    add a file to staging area in stagingArea/toTrack folder.
     */
    public static void add(String fileName){
        File FILE_CWD_DIR = join(CWD, fileName); //File is in CWD. Files in .gitlet/Blobs are files saved by gitlet.
        if(!FILE_CWD_DIR.exists()){
            System.out.println("File does not exist.");
            System.exit(0);
        }
        File STAGING_DIR = join(".gitlet", "stagingArea");
        File STAGINGAREA_DIR = join(".gitlet", "stagingArea", "toTrack");

        /*
        build the stagingArea folder
         */
        if(!STAGING_DIR.exists()){
            STAGING_DIR.mkdir();
        }

        /*
        build the stagingArea/toTrack folder
         */
        if(!STAGINGAREA_DIR.exists()){
            STAGINGAREA_DIR.mkdir();
        }

        /*
        head is initialized (null) for a new command. Need to find head in the file
         */
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        Commit currComm = Commit.fromFile(currCommSHA1);
        String fileSHA1 = Utils.sha1(readContents(FILE_CWD_DIR));
        File FILE_STAGINGAREA = join(STAGINGAREA_DIR, fileName);

        /*
        If the current working version of the file is identical to the version in the current commit, do not stage
        it to be added, and remove it from the staging area if it is already there
         */
        if(currComm.Blobs.containsKey(fileName) && fileSHA1.equals(currComm.Blobs.get(fileName))){
            /** if the file is removed (but not committed yet) and added back, we unremove the file (delete the file in toRemove directory). */
            File fileREMOVEAREA_DIR = join(".gitlet", "stagingArea", "toRemove", fileName);
            if(fileREMOVEAREA_DIR.exists() && Utils.sha1(readContents(fileREMOVEAREA_DIR)).equals(Utils.sha1(readContents(FILE_CWD_DIR))) ){
                fileREMOVEAREA_DIR.delete();
            }
            if(FILE_STAGINGAREA.exists()){
                FILE_STAGINGAREA.delete();
            }
            return;
        }

        writeContents(FILE_STAGINGAREA, readContents(FILE_CWD_DIR));
    }

    /*
    remove the file from toTrack folder, and stage the file for removal in stagingArea/toRemove folder
     */
    public static void remove(String fileName){
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        Commit currComm = Commit.fromFile(currCommSHA1);
        File STAGINGAREA_DIR = join(".gitlet", "stagingArea", "toTrack");
        boolean isStaged = join(STAGINGAREA_DIR, fileName).exists();
        File FILE_CWD_DIR = join(CWD, fileName);
        boolean isTracked = currComm.Blobs.containsKey(fileName);

        /*
        the file is neither staged nor tracked by the head commit, print the error message "No reason to remove the file."
         */
        if(!STAGINGAREA_DIR.exists() || !isStaged){
            if(!isTracked){
                System.out.println("No reason to remove the file.");
                System.exit(0);
            }
        }

        /*
        Unstage the file if it is currently staged for addition.
         */
        if(isStaged){
            join(STAGINGAREA_DIR, fileName).delete();
        }

        /*
         If the file is tracked in the current commit, stage it for removal
         and remove the file from the working directory.
         */
        if(isTracked){
            File STAGING_DIR = join(".gitlet", "stagingArea");
            File STAGINGREMOVE_DIR = join(".gitlet", "stagingArea", "toRemove");
            if(!STAGING_DIR.exists()){
                STAGING_DIR.mkdir();
            }
            if(!STAGINGREMOVE_DIR.exists()){
                STAGINGREMOVE_DIR.mkdir();
            }
            File f = join(STAGINGREMOVE_DIR, fileName);
            if(FILE_CWD_DIR.exists()) { // should consider the case that the file is deleted by UNIX rm command
                writeContents(f, readContentsAsString(FILE_CWD_DIR));
            }
            else{
                writeContents(f, readContentsAsString(join(BLOBS_DIR, fileName, currComm.Blobs.get(fileName))));
            }
            if(FILE_CWD_DIR.exists()){
                FILE_CWD_DIR.delete();
            }
        }
    }

    /*
    Starting at the current head commit, display info. of each commit back to the initial commit.
     */
    public static void log(){
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        while(currSHA1 != null){
            Commit currComm = Commit.fromFile(currSHA1);
            System.out.println("===");
            System.out.println("commit " + currComm.SHA1());
            if(currComm.secondParentSHA1() != null){
                System.out.println("Merge: " + currComm.parentSHA1().substring(0,7) + " " + currComm.secondParentSHA1().substring(0,7));
            }
            System.out.println("Date: " + new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH).format(currComm.time()));
            System.out.println(currComm.message());
            System.out.println();
            currSHA1 = currComm.parentSHA1();
        }
    }

    /*
    display info. of all commits
     */
    public static void logGlobal(){
        File[] allCommits = COMMITS_DIR.listFiles();
        for(File f:allCommits){
            Commit fC = Commit.fromFile(f.getName());
            System.out.println("===");
            System.out.println("commit " + fC.SHA1());
            if(fC.secondParentSHA1() != null){
                System.out.println("Merge: " + fC.parentSHA1().substring(0,7) + " " + fC.secondParentSHA1().substring(0,7));
            }
            System.out.println("Date: " + new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH).format(fC.time()));
            System.out.println(fC.message());
            System.out.println();
        }
    }

    /*
    Prints out the ids of all commits that have the given commit message.
     */
    public static void find(String message){
        int n = 0;
        File[] allCommits = COMMITS_DIR.listFiles();
        for(File f:allCommits) {
            Commit fC = Commit.fromFile(f.getName());
            if(fC.message().equals(message)){
                n += 1;
                System.out.println(fC.SHA1());
            }
        }
        if(n == 0){
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status(){
        System.out.println("=== Branches ===");
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        System.out.println("*" + head); //marks the current branch with a *
        File[] allBranches = BRANCH_DIR.listFiles();
        for(File f:allBranches){
            if(!f.getName().equals(head) && !f.getName().equals("head")) {
                System.out.println(f.getName());
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        List<String> stagedFileName = new LinkedList<>();
        File STAGINGAREA_DIR = join(".gitlet", "stagingArea", "toTrack");
        File[] allStagedFile = STAGINGAREA_DIR.listFiles();
        if(STAGINGAREA_DIR.exists() && allStagedFile.length > 0) {
            for (File f : allStagedFile) {
                stagedFileName.add(f.getName());
            }
            Collections.sort(stagedFileName); //sort the file names in lexicographic order
            for (String s : stagedFileName) {
                System.out.println(s);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        List<String> removedFileName = new LinkedList<>();
        File STAGINGREMOVE_DIR = join(".gitlet", "stagingArea", "toRemove");
        File[] allRemovedFile = STAGINGREMOVE_DIR.listFiles();
        if(STAGINGREMOVE_DIR.exists() && allRemovedFile.length > 0) {
            for (File f : allRemovedFile) {
                removedFileName.add(f.getName());
            }
            Collections.sort(removedFileName);
            for (String s : removedFileName) {
                System.out.println(s);
            }
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    /*
    Takes the version of the file as it exists in the head commit and puts it in the working directory.
     */
    public static void checkout1(String fileName){
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        Commit currComm = Commit.fromFile(currCommSHA1);
        String fileSHA1 = currComm.Blobs.get(fileName);
        if(fileSHA1 == null){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File fileInBlobs = join(BLOBS_DIR, fileName, fileSHA1);
        File fileInCWD = join(CWD, fileName);
        writeContents(fileInCWD, readContents(fileInBlobs));
    }

    /*
    Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory.
     */
    public static void checkout2(String id, String fileName){

        /** no commit with the given id exists */
        int hasCommit = 0;
        if(!join(COMMITS_DIR, id).exists()){
            /** find complete ID for an abbreviated SHA1 */
            if(id.length() < UID_LENGTH){
                File[] allCommits = COMMITS_DIR.listFiles();
                for(File c:allCommits){
                    if(c.getName().startsWith(id)){
                        id = c.getName();
                        hasCommit = 1;
                        break;
                    }
                }
            }
            if(hasCommit == 0) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
        }

        Commit Comm = Commit.fromFile(id);
        String fileSHA1 = Comm.Blobs.get(fileName);
        if(fileSHA1 == null){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File fileInBlobs = join(BLOBS_DIR, fileName, fileSHA1);
        File fileInCWD = join(CWD, fileName);
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        Commit currComm = Commit.fromFile(currCommSHA1);
        if (fileInCWD.exists() && !currComm.Blobs.containsKey(fileName)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        writeContents(fileInCWD, readContentsAsString(fileInBlobs));
    }

    public static void checkout3(String branchName){
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        File branch_FILE = join(BRANCH_DIR, branchName);
        if(!branch_FILE.exists()){
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String commitSHA1 = readContentsAsString(join(BRANCH_DIR, branchName)); //SHA1 for the head commit of the branch named as branchName
        checkout3Help(branchName, currCommSHA1 ,commitSHA1);
    }

    /*
    Takes all files in the commit at the head of the given branch, and puts them in the working directory.
     */
    private static void checkout3Help(String branchName, String currCommSHA1, String checkedOutCommitSHA1){


        head = readContentsAsString(join(BRANCH_DIR, "head"));
        Commit currComm = Commit.fromFile(currCommSHA1);

        /*
        that branch is the current branch
         */
        if(branchName.equals(head) && checkedOutCommitSHA1.equals(currCommSHA1)){
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        if(!checkedOutCommitSHA1.equals(currCommSHA1)) {
            Commit Comm = Commit.fromFile(checkedOutCommitSHA1); //the head commit of the branch named as branchName
            //int hasUntracked; // to determine whether there are untracked files in the current branch that would be overwritten
            File[] filesInCWD = CWD.listFiles();
            for (File f : filesInCWD) {
                String fName = f.getName();
                /** If a working file is untracked in the current branch and would be overwritten by the checkout. */
                if (Comm.Blobs.containsKey(fName) && !currComm.Blobs.containsKey(fName)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
            for (String fileName : Comm.Blobs.keySet()) {
                checkout2(checkedOutCommitSHA1, fileName);
            }

            /*
            Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
             */
            for (String fileName : currComm.Blobs.keySet()) {
                if (!Comm.Blobs.containsKey(fileName)) {
                    File fileCWD = join(CWD, fileName);
                    fileCWD.delete();
                }
            }
        }
        head = branchName;
        File head_FILE = Utils.join(BRANCH_DIR, "head");
        writeContents(head_FILE, head);
    }

    /*
    Creates a new branch with the given name, and points it at the current head commit.
     */
    public static void createNewBranch(String branchName){
        File newBranchFile = join(BRANCH_DIR, branchName);
        if(newBranchFile.exists()){
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));

        writeContents(newBranchFile, currCommSHA1);

    }

    /*
    Deletes the branch with the given name.
     */
    public static void removeBranch(String branchName){
        File BranchFile = join(BRANCH_DIR, branchName);
        if(!BranchFile.exists()){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        head = readContentsAsString(join(BRANCH_DIR, "head"));

        /** cannot remove the branch you???re currently on. */
        if(branchName.equals(head)){
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        BranchFile.delete();
    }

    /*
    Checks out all the files tracked by the given commit.
     */
    public static void reset(String commitID){
        File Commit_FILE = join(COMMITS_DIR, commitID);
        if(!Commit_FILE.exists()){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        File currBranchHead = join(BRANCH_DIR, head);
        String currCommSHA1 = readContentsAsString(currBranchHead);

        checkout3Help(head, currCommSHA1, commitID);
        /** moves the current branch???s head to that commit node.*/
        writeContents(currBranchHead, commitID);
        clearStagingArea();
    }

    public static void getParent(String sha1){
        Commit Comm = Commit.fromFile(sha1);
        System.out.println(Comm.parentSHA1());
    }

    /*
    save the SHA1 for the head of the active branch, which HEAD points to.
     */
    public static void saveHead(String s){
        head = readContentsAsString(join(BRANCH_DIR, "head"));
        File activeBranch_FILE = Utils.join(BRANCH_DIR, head);
        writeContents(activeBranch_FILE, s);
        //File head_FILE = Utils.join(BRANCH_DIR, "head");
        //writeContents(head_FILE, head);
    }

    public static void merge(String branchName) {
        File stagingarea = join(".gitlet", "stagingArea", "toTrack");
        File removearea = join(".gitlet", "stagingArea", "toRemove");
        if((stagingarea.exists() && stagingarea.listFiles().length > 0) || (removearea.exists() && removearea.listFiles().length > 0)){
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        File branchFile = join(BRANCH_DIR, branchName);
        if(!branchFile.exists()){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        head = readContentsAsString(join(BRANCH_DIR, "head"));
        if(head.equals(branchName)){
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }


        int isConflict = 0;


        String givenBranch = readContentsAsString(branchFile);
        Commit givenBranchC = Commit.fromFile(givenBranch);
        String currCommSHA1 = readContentsAsString(Utils.join(BRANCH_DIR, head));
        Commit currComm = Commit.fromFile(currCommSHA1);
        String splitPoint = findSplitPoint(givenBranch, currCommSHA1);
        Commit splitPointC = Commit.fromFile(splitPoint);
        if(splitPoint.equals(givenBranch)){
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        if(splitPoint.equals(currCommSHA1)){
            checkout3(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        HashSet<String> filesToConsider = new HashSet<>();
        filesToConsider.addAll(splitPointC.Blobs.keySet());
        filesToConsider.addAll(givenBranchC.Blobs.keySet());
        filesToConsider.addAll(currComm.Blobs.keySet());

        for(String fName:filesToConsider){
            if(!splitPointC.Blobs.containsKey(fName)){
                if(!givenBranchC.Blobs.containsKey(fName)){ //rule 4: not present at the split point and are present only in the current branch
                    continue;
                }
                if(!currComm.Blobs.containsKey(fName)){ //rule 5: not present at the split point and are present only in the given branch
                    checkout2(givenBranch, fName);
                    add(fName);
                    continue;
                }

                /*
                Coming to here, means that both givenBranch and currComm have the file
                 */
                if(!givenBranchC.Blobs.get(fName).equals(currComm.Blobs.get(fName))){ //rule 8
                    writeConflict(fName, givenBranchC.Blobs.get(fName), currComm.Blobs.get(fName) );
                    isConflict = 1;
                    continue;
                }
            }

            /*
            file is in split point
             */
            else {
                String splfileSHA1 = splitPointC.Blobs.get(fName);
                String givenfileSHA1 = givenBranchC.Blobs.get(fName);
                String currfileSHA1 = currComm.Blobs.get(fName);
                if(currfileSHA1 == null){
                    if(givenfileSHA1 == null){ //rule 3: modified in both the current and given branch in the same way
                        continue;
                    }
                    /* rule 7: present at the split point, unmodified in the given branch, and absent
                      in the current branch */
                    if(splfileSHA1.equals(givenfileSHA1)){
                        continue;
                    }
                    else{ //absent in current branch but modified in given branch, rule 8
                        writeConflict(fName, givenfileSHA1, currfileSHA1);
                        isConflict = 1;
                        continue;
                    }
                }

                if(givenfileSHA1 == null){
                    // currfileSHA1 == null doesn't need to be checked, because it is in the previous texts
                    if(splfileSHA1.equals(currfileSHA1)){ //rule 6
                        remove(fName);
                        continue;
                    }
                    else{
                        if(currfileSHA1 == null){
                            continue;
                        }

                        //absent in given branch but modified in current branch, rule 8: modified in different ways in the current and given branches
                        writeConflict(fName, givenfileSHA1, currfileSHA1);
                        isConflict = 1;
                        continue;
                    }
                }

                if(splfileSHA1.equals(currfileSHA1)){ //not modified in current branch (head)
                    if(!splfileSHA1.equals(givenfileSHA1)){ //rule 1: modified in the given branch, not modified in the current branch
                        checkout2(givenBranch, fName);
                        add(fName);
                        continue;
                    }
                    else{ // not modified in given branch, either
                        continue;
                    }
                }

                if(splfileSHA1.equals(givenfileSHA1)) { //not modified in given branch
                    if(!splfileSHA1.equals(currfileSHA1)){ //rule 2: modified in the current branch but not in the given branch
                        continue;
                    }
                }

                if(!splfileSHA1.equals(currfileSHA1) && !splfileSHA1.equals(givenfileSHA1) ){
                    if(currfileSHA1.equals(givenfileSHA1)){ //rule 3: modified in both the current and given branch in the same way
                        continue;
                    }
                    else{ // current branch and given branch modify in different ways, rule 8
                        writeConflict(fName, givenfileSHA1, currfileSHA1);
                        isConflict = 1;
                        continue;
                    }
                }
            }
        }

        makeMergeCommit("Merged " + branchName + " into " + head + " .", currCommSHA1, givenBranch );
        if(isConflict == 1){
            System.out.println("Encountered a merge conflict.");
        }


    }

    /*
    write in the conflicted file with fileName
     */

    private static void writeConflict(String fName, String fileSHA1GivenBranch, String fileSHA1Curr) {
        File file = join(CWD, fName);

        StringBuilder contentBuilder = new StringBuilder();

        contentBuilder.append("<<<<<<< HEAD").append(System.getProperty( "line.separator" ));
        if(fileSHA1Curr != null && join(BLOBS_DIR, fName, fileSHA1Curr).exists()) {
            contentBuilder.append(readContentsAsString(join(BLOBS_DIR, fName, fileSHA1Curr))).append(System.getProperty( "line.separator" ));

        }
        contentBuilder.append("=======").append(System.getProperty( "line.separator" ));
        if(fileSHA1GivenBranch != null && join(BLOBS_DIR, fName, fileSHA1GivenBranch).exists()) {
            contentBuilder.append(readContentsAsString(join(BLOBS_DIR, fName, fileSHA1GivenBranch))).append(System.getProperty( "line.separator" ));
        }
        contentBuilder.append(">>>>>>>").append(System.getProperty( "line.separator" ));
        writeContents(file, contentBuilder.toString());

    }

    /*
    find the split Commit SHA1 for commit with C1sha1 and another commit with C2sha1
     */
    private static String findSplitPoint(String C1sha1, String C2sha1){
        TreeMap<Date, String> parents1 = new TreeMap<>(Collections.reverseOrder());
        parents1.putAll(ancestors(C1sha1));// all the parents for commit 1, in the reverse time order (from latest to oldest)
        TreeMap<Date, String> parents2 = new TreeMap<>(ancestors(C2sha1)); // all the parents for commit 2


        for(Date d:parents1.keySet()){
            String sha1 = parents1.get(d);
            if(parents2.containsValue(sha1)){
                return sha1;
            }
        }
        return null;

    }

    /*
    find the ancestors (parents) of the commit with sha1 including itself
     */
    private static HashMap<Date, String> ancestors(String sha1){

        Commit thisCommit = Commit.fromFile(sha1);
        String parentsha1 = thisCommit.parentSHA1();
        HashMap<Date, String> anc = new HashMap<>();
        anc.put(thisCommit.time(), sha1); // should contain the commit itself, for special merge cases (given/current branch is the split point)
        if(parentsha1 == null){
            return anc; // cannot return null, or error will occur.
        }
        else {
            Commit parent = Commit.fromFile(parentsha1);
            anc.put(parent.time(), parentsha1);
            anc.putAll(ancestors(parentsha1));
            if(thisCommit.secondParentSHA1() != null){ // Second parent should also be included if the commit has one.
                Commit secondParent = Commit.fromFile(thisCommit.secondParentSHA1());
                anc.put(secondParent.time(), secondParent.SHA1());
                anc.putAll(ancestors(thisCommit.secondParentSHA1()));
            }
        }
        return anc;
    }

    /*
    clear the staging area
    */
    public static void clearStagingArea() {
        File stagingarea = join(".gitlet", "stagingArea", "toTrack");
        String[] entries = stagingarea.list();
        if (entries != null) {
            for (String s : entries) {
                File currentFile = new File(stagingarea.getPath(), s);
                currentFile.delete();
            }
            stagingarea.delete();
        }


        File removeFile = join(".gitlet", "stagingArea", "toRemove");
        String[] entriesRemove = removeFile.list();
        if (entriesRemove != null) {
            for (String s : entriesRemove) {
                File currentFile = new File(removeFile.getPath(), s);
                currentFile.delete();
            }
            removeFile.delete();
        }
        join(".gitlet", "stagingArea").delete();
    }


}
