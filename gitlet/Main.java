package gitlet;
import java.io.IOException;

import static gitlet.Utils.*;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author yyf
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];

        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                if(Repository.GITLET_DIR.exists()){
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                }

                Repository.setupPersistence();
                Repository.makeInitialCommit();
                break;
            case "add":
                validateNumArgs(args, 2);
                if(args.length != 2){
                    System.out.println("File does not exist.");
                    System.exit(0);
                }
                String fileName = args[1];
                Repository.add(fileName);

                break;
            case "commit":
                validateNumArgs(args, 2);
                String message = args[1];
                if(message.equals("")){
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.makeCommit(message);

                break;
            case "rm":
                validateNumArgs(args, 2);
                String filetoRemove = args[1];
                Repository.remove(filetoRemove);
                break;
            case "log":
                validateNumArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                Repository.logGlobal();
                break;
            case "find":
                validateNumArgs(args, 2);
                String messageToFind = args[1];
                Repository.find(messageToFind);
                break;
            case "status":
                checkInitialized();
                validateNumArgs(args, 1);
                Repository.status();
                break;
            case "checkout":
                checkInitialized();
                if(args.length == 3){
                    if(!args[1].equals("--")){
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    String fileName1 = args[2];
                    Repository.checkout1(fileName1);
                }
                else if(args.length == 4){
                    if(!args[2].equals("--")){
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    String id = args[1];
                    String fileName2 = args[3];
                    Repository.checkout2(id, fileName2);
                }
                else if(args.length == 2){
                    String branchName = args[1];
                    Repository.checkout3(branchName);
                }
                else{
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                validateNumArgs(args, 2);
                String branchName = args[1];
                Repository.createNewBranch(branchName);
                break;
            //self-defined
            case "getParent":
                validateNumArgs(args, 2);
                String sha1 = args[1];
                Repository.getParent(sha1);
                break;
            case "rm-branch":
                validateNumArgs(args, 2);
                checkInitialized();
                String branchNameRemove = args[1];
                Repository.removeBranch(branchNameRemove);
                break;
            case "reset":
                validateNumArgs(args, 2);
                checkInitialized();
                String commitID = args[1];
                Repository.reset(commitID);
                break;
            case "merge":
                validateNumArgs(args, 2);
                checkInitialized();
                String branchN = args[1];
                Repository.merge(branchN);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);

        }
    }

    /*
    If arguments number not equals to n, abort.
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void checkInitialized(){
        if(!Repository.GITLET_DIR.exists()){
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
