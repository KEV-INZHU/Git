package gitlet;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author KEVIN ZHU
 * @collab WILLIAM HU
 * @source lab 6
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        Repository rep = new Repository();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (!args[0].equals("init") && !Utils.join(Repository.CWD, ".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (validateNumArgs("init", args, 1)) {
                    rep.init();
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "add":
                if (validateNumArgs("add", args, 2)) {
                    rep.add(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "commit":
                if (validateNumArgs("commit", args, 1)) {
                    System.out.println("Please enter a commit message.");
                } else if (validateNumArgs("commit", args, 2)
                        && args[1].length() == 0) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                } else if (validateNumArgs("commit", args, 2)) {
                    rep.commit(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "checkout":
                rep.checkout(args);
                break;
            case "log":
                if (validateNumArgs("log", args, 1)) {
                    rep.log();
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "status":
                if (validateNumArgs("status", args, 1)) {
                    rep.status();
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "find":
                if (validateNumArgs("find", args, 2)) {
                    rep.find(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "rm":
                if (validateNumArgs("rm", args, 2)) {
                    rep.remove(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "global-log":
                if (validateNumArgs("global", args, 1)) {
                    rep.global();
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "branch":
                if (validateNumArgs("branch", args, 2)) {
                    rep.branch(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "reset":
                if (validateNumArgs("reset", args, 2)) {
                    rep.reset(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "rm-branch":
                if (validateNumArgs("rm-branch", args, 2)) {
                    rep.removeBranch(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            case "merge":
                if (validateNumArgs("merge", args, 2)) {
                    rep.merge(args[1]);
                    break;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                    break;
                }
            default:
                System.out.println("No command with that name exists.");
                break;
        }

    }

    private static boolean validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            return false;
        }
        return true;
    }
}
