package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author KEVIN ZHU
 */
public class Repository implements Serializable {
    File commit = join(GITLET_DIR, "commit");
    File blob = join(GITLET_DIR, "blob");
    File stage = join(GITLET_DIR, "stage");
    File removed = join(GITLET_DIR, "remove");
    File branch = join(GITLET_DIR, "branch");
    File HEAD = join(GITLET_DIR, "HEAD");

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdir();
            blob.mkdir();
            commit.mkdir();
            stage.mkdir();
            branch.mkdir();
            removed.mkdir();
            Commit initial = new Commit("initial commit", null, new HashMap<String, String>());
            initial.initialTimeSet();
            File file = new File(commit, initial.getId());
            writeObject(file, initial);
            File bran = new File(branch, "master");
            writeContents(bran, initial.getId());
            writeContents(HEAD, "master");
        }
    }

    public void add(String fileName) {
        File file = new File(fileName);
        boolean cwdExists = new File(CWD, fileName).exists();
        if (!cwdExists) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            File stagePointer = new File(stage, fileName);
            String fileContents = readContentsAsString(file);
            boolean stageExists = new File(stage, fileName).exists();
            if (!stageExists) {
                writeContents(stagePointer, readContentsAsString(file));
            }
            String stageContents = readContentsAsString(stagePointer);
            boolean sameCheck = stageContents.equals(fileContents);
            if (!sameCheck) {
                writeContents(stagePointer, readContentsAsString(file));
            }
            Commit mostRecentCommit = getNewestCommit();
            HashMap<String, String> tracked = mostRecentCommit.getTracked();
            boolean isEmptyBlob = Objects.requireNonNull(plainFilenamesIn(blob)).isEmpty();
            boolean inBlobMap = tracked.containsKey(fileName);
            if (!isEmptyBlob && inBlobMap) {
                File blobPointer = new File(blob, tracked.get(fileName));
                String blobContents = readContentsAsString(blobPointer);
                File removePointer = new File(removed, fileName);
                boolean blobCheck = blobContents.equals(fileContents);
                if (blobCheck) {
                    stagePointer.delete();
                    removePointer.delete();
                }
            }
        }
    }

    public void commit(String message) {
        if ((plainFilenamesIn(stage)).isEmpty() && (plainFilenamesIn(removed)).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            Set<String> stageSet = new HashSet<>(Arrays.asList(stage.list()));
            Set<String> removeSet = new HashSet<>(Arrays.asList(removed.list()));
            Commit mostRecentCommit = getNewestCommit();
            HashMap<String, String> tracked = mostRecentCommit.getTracked();
            HashMap<String, String> trackCopy = new HashMap<>();
            for (String fileName : tracked.keySet()) {
                if (!stageSet.contains(fileName) && !removeSet.contains(fileName)) {
                    trackCopy.put(fileName, tracked.get(fileName));
                }
            }
            for (String fileName : stageSet) {
                File file = new File(stage, fileName);
                File writeFile = new File(blob, getId(file));
                writeContents(writeFile, readContentsAsString(file));
                trackCopy.put(fileName, getId(file));
            }
            clear(stage);
            clear(removed);
            Commit newCommit = new Commit(message, mostRecentCommit.getId(), trackCopy);
            if (message.contains("Merge")) {
                String[] second = message.split(" ");
                File branchPointer = new File(branch, second[1]);
                String branchId = readContentsAsString(branchPointer);
                newCommit.setMergeParent(branchId);
            }
            File file = new File(commit, newCommit.getId());
            writeObject(file, newCommit);
            File bran = new File(branch, readContentsAsString(HEAD));
            writeContents(bran, newCommit.getId());
        }
    }

    public void checkout(String[] args) {
        if (args.length == 2) {
            String branchName = args[1];
            List<String> branchList = plainFilenamesIn(branch);
            File bran = new File(this.branch, branchName);
            String branchHead = readContentsAsString(HEAD);
            if (!branchList.contains(branchName)) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            if (branchHead.equals(branchName)) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            String branchId = readContentsAsString(bran);
            checkoutBranch(branchId);
            writeContents(HEAD, branchName);
        } else if (args.length == 3) {
            String fileName = args[2];
            checkoutFileName(fileName);
        } else if (args.length == 4 && args[2].equals("--")) {
            String commitId = args[1];
            String fileName = args[3];
            checkoutCommitId(commitId, fileName);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private void checkoutBranch(String branchId) {
        Set<String> cwdSet = new HashSet<>(Arrays.asList(CWD.list()));
        HashMap<String, String> tracked = getNewestCommit().getTracked();
        HashMap<String, String> branchTracked = getCommit(branchId).getTracked();
        for (String fileName : cwdSet) {
            File current = new File(CWD, fileName);
            if (!current.isDirectory()) {
                if (tracked.containsKey(fileName) && !branchTracked.containsKey(fileName)
                        && plainFilenamesIn(removed).contains(fileName)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                } else if (!tracked.containsKey(fileName)
                        && branchTracked.containsKey(fileName)) {
                    System.out.println("There is an untracked file in the way; delete it,"
                            + " or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        for (String fileName : tracked.keySet()) {
            if (!branchTracked.containsKey(fileName)) {
                File file = new File(CWD, fileName);
                file.delete();
            }
        }
        for (Map.Entry<String, String> entry : branchTracked.entrySet()) {
            String fileName = entry.getKey();
            String fileSha1 = entry.getValue();
            File fileMaker = new File(CWD, fileName);
            File fileContentPointer = new File(blob, fileSha1);
            String fileContents = (readContentsAsString(fileContentPointer));
            writeContents(fileMaker, fileContents);
        }
        clear(stage);
        clear(removed);
    }

    private void checkoutFileName(String fileName) {
        Commit mostRecentCommit = getNewestCommit();
        HashMap<String, String> tracked = mostRecentCommit.getTracked();
        if (tracked.containsKey(fileName)) {
            String value = tracked.get(fileName);
            File blobFile = new File(blob, value);
            String blobContents = readContentsAsString(blobFile);
            File checkoutFile = new File(CWD, fileName);
            writeContents(checkoutFile, blobContents);
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    private void checkoutCommitId(String commitId, String fileName) {
        if (getCommit(commitId) != null) {
            Commit oldCommit = getCommit(commitId);
            HashMap<String, String> tracked = oldCommit.getTracked();
            if (tracked.containsKey(fileName)) {
                String value = tracked.get(fileName);
                File blobFile = new File(blob, value);
                String blobContents = readContentsAsString(blobFile);
                File checkoutFile = new File(CWD, fileName);
                writeContents(checkoutFile, blobContents);
                return;
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public void status() {
        System.out.println("=== Branches ===");
        Set<String> branchSet = new HashSet<>(Arrays.asList(branch.list()));
        List<String> sortedBranch = branchSet.stream().sorted().collect(Collectors.toList());
        for (String fileName : sortedBranch) {
            if (fileName.equals(readContentsAsString(HEAD))) {
                System.out.println("*" + readContentsAsString(HEAD));
            } else {
                System.out.println(fileName);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        Set<String> stageSet = new HashSet<>(Arrays.asList(stage.list()));
        List<String> sortedSet = stageSet.stream().sorted().collect(Collectors.toList());
        for (String fileName : sortedSet) {
            System.out.println(fileName);
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        Set<String> removeSet = new HashSet<>(Arrays.asList(removed.list()));
        List<String> sortedRemove = removeSet.stream().sorted().collect(Collectors.toList());
        for (String fileName : sortedRemove) {
            System.out.println(fileName);
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> sortedModified = modified().stream().sorted().collect(Collectors.toList());
        for (String fileName : sortedModified) {
            System.out.println(fileName);
        }
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        List<String> sortedUntracked = untracked().stream().sorted().collect(Collectors.toList());
        for (String fileName : sortedUntracked) {
            System.out.println(fileName);
        }
        System.out.println("");
        System.out.println("");
    }

    public void log() {
        Commit mostRecentCommit = getNewestCommit();
        while (mostRecentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + mostRecentCommit.getId());
            if (mostRecentCommit.getMessage().contains("Merged")) {
                String mergeMsg = mostRecentCommit.getMessage();
                String[] second = mergeMsg.split(" ");
                File branchPointer = new File(branch, second[1]);
                String branchId = readContentsAsString(branchPointer);
                Commit branchCommit = getCommit(branchId);
                System.out.println("Merge: " + mostRecentCommit.getParent().substring(0, 7)
                        + " " + branchCommit.getParent().substring(0, 7));
            }
            System.out.println("Date: " + mostRecentCommit.getTime());
            System.out.println(mostRecentCommit.getMessage() + "\n");
            if (mostRecentCommit.getParent() != null) {
                File commitName = new File(commit, mostRecentCommit.getParent());
                mostRecentCommit = readObject(commitName, Commit.class);
            } else {
                break;
            }
        }
    }

    public void branch(String branchName) {
        File branchPointer = new File(branch, branchName);
        if (branchPointer.exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            Commit mostRecentCommit = getNewestCommit();
            String commitId = mostRecentCommit.getId();
            writeContents(branchPointer, commitId);
        }
    }

    public void find(String commitMessage) {
        Set<String> commitSet = new HashSet<>(Arrays.asList(commit.list()));
        boolean found = false;
        for (String fileName : commitSet) {
            File commitPointer = new File(commit, fileName);
            Commit current = readObject(commitPointer, Commit.class);
            if (current.getMessage().equals(commitMessage)) {
                System.out.println(current.getId());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void remove(String fileName) {
        HashMap<String, String> tracked = getNewestCommit().getTracked();
        Set<String> blobSet = new HashSet<>(Arrays.asList(blob.list()));
        Set<String> stageSet = new HashSet<>(Arrays.asList(stage.list()));
        boolean stageFound = false;
        if (stageSet.contains(fileName)) {
            stageFound = true;
            File file = new File(stage, fileName);
            file.delete();
        }
        boolean trackFound = false;
        if (tracked.containsKey(fileName)) {
            trackFound = true;
            File removeCurr = new File(CWD, fileName);
            if (removeCurr.exists()) {
                removeCurr.delete();
            }
            String removeSha1 = tracked.get(fileName);
            for (String fileN : blobSet) {
                if (fileN.equals(removeSha1)) {
                    File blobPointer = new File(blob, removeSha1);
                    String removeContents = readContentsAsString((blobPointer));
                    File moveTo = new File(removed, fileName);
                    writeContents(moveTo, removeContents);
                }
            }
        }
        if (stageFound || trackFound) {
            return;
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    public void global() {
        Set<String> commitSet = new HashSet<>(Arrays.asList(commit.list()));
        for (String fileName : commitSet) {
            File commitPointer = new File(commit, fileName);
            Commit current = readObject(commitPointer, Commit.class);
            System.out.println("===");
            System.out.println("commit " + current.getId());
            System.out.println("Date: " + current.getTime());
            System.out.println(current.getMessage() + "\n");
        }
    }

    public void reset(String commitId) {
        if (getCommit(commitId) == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        checkoutBranch(commitId);
        File branchPointer = new File(branch, readContentsAsString(HEAD));
        writeContents(branchPointer, getCommit(commitId).getId());
    }

    public void removeBranch(String branchName) {
        String headPointer = readContentsAsString(HEAD);
        if (!plainFilenamesIn(branch).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(headPointer)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File rmBranch = new File(branch, branchName);
        rmBranch.delete();
    }

    public void merge(String branchName) {
        List<String> branchList = plainFilenamesIn(branch);
        List<String> stageList = plainFilenamesIn(stage);
        List<String> removeList = plainFilenamesIn(removed);
        String mergedConflict = "";
        if (mergeError(stageList, removeList, branchList, branchName)) {
            System.exit(0);
        } else {
            Commit splitPoint = splitPoint(branchName);
            File branchPointer = new File(branch, branchName);
            Commit branchCommit = getCommit(readContentsAsString(branchPointer));
            HashMap<String, String> tracked = getNewestCommit().getTracked();
            HashMap<String, String> splitTracked = splitPoint.getTracked();
            HashMap<String, String> branchTracked = branchCommit.getTracked();
            Set<String> allFiles = new HashSet<>(tracked.keySet());
            allFiles.addAll(splitTracked.keySet());
            allFiles.addAll(branchTracked.keySet());
            for (String fileName : allFiles) {
                if (splitTracked.containsKey(fileName) && tracked.containsKey(fileName)
                        && branchTracked.containsKey(fileName)) {
                    if (splitTracked.get(fileName).equals(tracked.get(fileName))
                            && !branchTracked.get(fileName).equals(tracked.get(fileName))) {
                        checkoutCommitId(branchCommit.getId(), fileName);
                        add(fileName);
                    } else if (splitTracked.get(fileName).equals(branchTracked.get(fileName))
                            && !tracked.get(fileName).equals(branchTracked.get(fileName))) {
                        add(fileName);
                    } else if (mergeHelper2(tracked, splitTracked, branchTracked, fileName)) {
                        mergedConflict += "conflict ";
                    } else if (!splitTracked.get(fileName).equals(tracked.get(fileName))
                            && !branchTracked.get(fileName).equals(tracked.get(fileName))) {
                        File trackBlobPointer = new File(blob, tracked.get(fileName));
                        File branchBlobPointer = new File(blob, branchTracked.get(fileName));
                        File cwdPointer = new File(CWD, fileName);
                        mergedConflict += "conflict ";
                        if (trackBlobPointer.exists() && branchBlobPointer.exists()) {
                            String trackContents = readContentsAsString(trackBlobPointer);
                            String branchContents = readContentsAsString(branchBlobPointer);
                            mergeConflict(cwdPointer, trackContents, branchContents);
                        } else if (!trackBlobPointer.exists() && branchBlobPointer.exists()) {
                            String branchContents = readContentsAsString(branchBlobPointer);
                            mergeConflict(cwdPointer, "", branchContents);
                        } else {
                            String trackContents = readContentsAsString(trackBlobPointer);
                            mergeConflict(cwdPointer, trackContents, "");
                        }
                    }
                } else if (mergeHelper1(tracked, splitTracked, branchTracked,
                        fileName)) {
                    mergedConflict += "conflict ";
                } else if (!splitTracked.containsKey(fileName)) {
                    if (!tracked.containsKey(fileName)) {
                        checkoutCommitId(branchCommit.getId(), fileName);
                        add(fileName);
                    } else {
                        checkoutCommitId(getNewestCommit().getId(), fileName);
                        add(fileName);
                    }
                }
            }
        }
        mergeHelper3(mergedConflict, branchName);
    }
    private boolean mergeError(List<String> stageList, List<String> removeList,
                               List<String> branchList, String branchName) {
        if (!stageList.isEmpty() || !removeList.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (!branchList.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        } else if (untrackedError(branchName)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
            return true;
        } else if (splitPoint(branchName) != null) {
            Commit splitPoint = splitPoint(branchName);
            File branchPointer = new File(branch, branchName);
            if (splitPoint.getId().equals(readContentsAsString(branchPointer))) {
                System.out.println("Given branch is an ancestor of the current branch.");
                return true;
            } else if (splitPoint.getId().equals(getNewestCommit().getId())) {
                File bran = new File(branch, branchName);
                checkoutBranch(readContentsAsString(bran));
                writeContents(bran, readContentsAsString(bran));
                System.out.println("Current branch fast-forwarded.");
                return true;
            }
        }
        return false;
    }
    private boolean mergeHelper1(HashMap<String, String> tracked,
                                 HashMap<String, String> splitTracked,
                                 HashMap<String, String> branchTracked, String fileName) {
        if (splitTracked.containsKey(fileName) && tracked.containsKey(fileName)
                && !branchTracked.containsKey(fileName)
                || splitTracked.containsKey(fileName)
                && branchTracked.containsKey(fileName) && !tracked.containsKey(fileName)) {
            if (!splitTracked.get(fileName).equals(tracked.get(fileName))
                    && !branchTracked.containsKey(fileName)) {
                File trackBlobPointer = new File(blob, tracked.get(fileName));
                File cwdPointer = new File(CWD, fileName);
                String trackContents = readContentsAsString(trackBlobPointer);
                mergeConflict(cwdPointer, trackContents, "");
                return true;
            } else if (splitTracked.get(fileName).equals(tracked.get(fileName))
                    && !branchTracked.containsKey(fileName)) {
                remove(fileName);
                return false;
            } else if (!splitTracked.get(fileName).equals(branchTracked.get(fileName))
                    && !tracked.containsKey(fileName)) {
                File cwdPointer = new File(CWD, fileName);
                File branchBlobPointer = new File(blob, branchTracked.get(fileName));
                String branchContents = readContentsAsString(branchBlobPointer);
                mergeConflict(cwdPointer, "", branchContents);
                return true;
            } else if (splitTracked.get(fileName).equals(branchTracked.get(fileName))
                    && !tracked.containsKey(fileName)) {
                if (plainFilenamesIn(CWD).contains(fileName)) {
                    remove(fileName);
                    return false;
                }
            }
        }
        return false;
    }
    private boolean mergeHelper2(HashMap<String, String> tracked,
                                 HashMap<String, String> splitTracked,
                                 HashMap<String, String> branchTracked, String fileName) {
        if (!splitTracked.get(fileName).equals(branchTracked.get(fileName))
                && !branchTracked.get(fileName).equals(tracked.get(fileName))) {
            File trackBlobPointer = new File(blob, tracked.get(fileName));
            File branchBlobPointer = new File(blob, branchTracked.get(fileName));
            File cwdPointer = new File(CWD, fileName);
            if (trackBlobPointer.exists() && branchBlobPointer.exists()) {
                String trackContents = readContentsAsString(trackBlobPointer);
                String branchContents = readContentsAsString(branchBlobPointer);
                mergeConflict(cwdPointer, trackContents, branchContents);
                return true;
            } else if (!trackBlobPointer.exists() && branchBlobPointer.exists()) {
                String branchContents = readContentsAsString(branchBlobPointer);
                mergeConflict(cwdPointer, "", branchContents);
                return true;
            } else {
                String trackContents = readContentsAsString(trackBlobPointer);
                mergeConflict(cwdPointer, trackContents, "");
                return true;
            }
        }
        return false;
    }
    private void mergeHelper3(String mergedConflict, String branchName) {
        if (mergedConflict.length() > 0) {
            System.out.println("Encountered a merge conflict.");
        }
        commit("Merged " + branchName + " into " + readContentsAsString(HEAD) + ".");
    }

    private Commit splitPoint(String branchName) {
        Commit current = getNewestCommit();
        File branchPointer = new File(branch, branchName);
        if (branchPointer.exists()) {
            Commit branchCommit = getCommit(readContentsAsString(branchPointer));
            Set<String> commitsTracker = new HashSet<>();
            commitsTracker.add(current.getId());
            if (commitsTracker.contains(branchCommit.getId())) {
                return branchCommit;
            }
            if (branchCommit.getMergeParent() != null) {
                if (!commitsTracker.contains(branchCommit.getMergeParent())) {
                    commitsTracker.add(branchCommit.getMergeParent());
                }
                return getCommit(branchCommit.getMergeParent());
            }
            commitsTracker.add(branchCommit.getId());
            while (current != null || branchCommit != null) {
                if (current.getParent() != null) {
                    if (!commitsTracker.contains(current.getParent())) {
                        commitsTracker.add(current.getParent());
                        if (current.getMergeParent() != null) {
                            if (!commitsTracker.contains(current.getMergeParent())) {
                                commitsTracker.add(current.getMergeParent());
                            } else {
                                return getCommit(current.getMergeParent());
                            }
                        }
                        current = getCommit(current.getParent());
                    } else {
                        return getCommit(current.getParent());
                    }
                }
                if (branchCommit.getParent() != null) {
                    if (!commitsTracker.contains(branchCommit.getParent())) {
                        commitsTracker.add(branchCommit.getParent());
                        if (branchCommit.getMergeParent() != null) {
                            if (!commitsTracker.contains(branchCommit.getMergeParent())) {
                                commitsTracker.add(branchCommit.getMergeParent());
                            } else {
                                return getCommit(branchCommit.getMergeParent());
                            }
                        }
                        branchCommit = getCommit(branchCommit.getParent());
                    } else {
                        return getCommit(branchCommit.getParent());
                    }
                }
            }
        }
        return null;
    }

    private boolean untrackedError(String branchName) {
        Commit splitPoint = splitPoint(branchName);
        HashMap<String, String> splitTracked = splitPoint.getTracked();
        HashMap<String, String> tracked = getNewestCommit().getTracked();
        File branchPointer = new File(branch, branchName);
        Commit branchCommit = getCommit(readContentsAsString(branchPointer));
        HashMap<String, String> branchTracked = branchCommit.getTracked();
        Set<String> untracked = untracked();
        for (String fileName : untracked) {
            if (tracked.containsKey(fileName) && !branchTracked.containsKey(fileName)
                    && !splitTracked.containsKey(fileName)
                    && plainFilenamesIn(removed).contains(fileName)) {
                return true;
            } else if (!tracked.containsKey(fileName) && branchTracked.containsKey(fileName)
                    && splitTracked.containsKey(fileName)) {
                return true;
            } else if (!tracked.containsKey(fileName)
                    && !plainFilenamesIn(stage).contains(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void mergeConflict(File file, String trackContents, String branchContents) {
        String newContents = ("<<<<<<< HEAD\n"
                + trackContents
                + "=======\n"
                + branchContents
                + ">>>>>>>\n");
        writeContents(file, newContents);
    }

    private Set<String> untracked() {
        Set<String> cwdSet = new HashSet<>(Arrays.asList(CWD.list()));
        Set<String> stageSet = new HashSet<>(Arrays.asList(stage.list()));
        Set<String> removeSet = new HashSet<>(Arrays.asList(removed.list()));
        HashMap<String, String> tracked = getNewestCommit().getTracked();
        Set<String> untrackedFiles = new HashSet<>();
        for (String fileName : cwdSet) {
            File file = new File(CWD, fileName);
            if (!file.isDirectory() && fileName.contains("txt")) {
                if (tracked.containsKey(fileName)) {
                    String sha1 = tracked.get(fileName);
                    File blobPointer = new File(blob, sha1);
                    String fileContents = readContentsAsString(blobPointer);
                    if (!fileContents.equals(readContentsAsString(file))) {
                        continue;
                    }
                } else if (!stageSet.contains(fileName) && !tracked.containsKey(fileName)) {
                    untrackedFiles.add(fileName);
                } else if (removeSet.contains(fileName)) {
                    untrackedFiles.add(fileName);
                }
            }
        }
        return untrackedFiles;
    }

    private Set<String> modified() {
        Set<String> cwdSet = new HashSet<>(Arrays.asList(CWD.list()));
        Set<String> stageSet = new HashSet<>(Arrays.asList(stage.list()));
        Set<String> removeSet = new HashSet<>(Arrays.asList(removed.list()));
        HashMap<String, String> tracked = getNewestCommit().getTracked();
        Set<String> modifiedFiles = new HashSet<>();
        for (String fileName : cwdSet) {
            File file = new File(CWD, fileName);
            if (!file.isDirectory()) {
                if (tracked.containsKey(fileName) && !stageSet.contains(fileName)) {
                    String sha1 = tracked.get(fileName);
                    File blobPointer = new File(blob, sha1);
                    String fileContents = readContentsAsString(blobPointer);
                    if (!fileContents.equals(readContentsAsString(file))) {
                        modifiedFiles.add(fileName + " (modified)");
                    }
                    if (readContentsAsString(file).contains("<<<<<<< HEAD")) {
                        modifiedFiles.remove(fileName + " (modified)");
                    }
                } else if (stageSet.contains(fileName)) {
                    File stagePointer = new File(stage, fileName);
                    File cwdPointer = new File(CWD, fileName);
                    if (!cwdPointer.exists()) {
                        modifiedFiles.add(fileName + " (deleted)");
                    } else if (!readContentsAsString(stagePointer).equals
                            (readContentsAsString(cwdPointer))) {
                        modifiedFiles.add(fileName + " (modified)");
                    }
                }
            }
        }
        for (String fileName : tracked.keySet()) {
            File file = new File(CWD, fileName);
            if (tracked.containsKey(fileName) && !removeSet.contains(fileName)) {
                if (!file.exists()) {
                    modifiedFiles.add(fileName + " (deleted)");
                }
            }
        }
        return modifiedFiles;
    }
    private void clear(File dir) {
        Set<String> clearSet = new HashSet<>(Arrays.asList(dir.list()));
        for (String fileName : clearSet) {
            File file = new File(dir, fileName);
            file.delete();
        }
    }

    private String getId(File file) {
        byte[] fileContents = readContents(file);
        byte[] tempByte = serialize(fileContents);
        return sha1(tempByte);
    }

    private Commit getNewestCommit() {
        File headLocation = new File(branch, readContentsAsString(HEAD));
        File commitName = new File(commit, readContentsAsString(headLocation));
        return readObject(commitName, Commit.class);
    }

    private Commit getCommit(String sha1) {
        Set<String> commitSet = new HashSet<>(Arrays.asList(commit.list()));
        for (String commitName : commitSet) {
            String shortId = commitName.substring(0, sha1.length());
            if (shortId.equals(sha1)) {
                File commitPointer = new File(commit, commitName);
                return readObject(commitPointer, Commit.class);
            }
        }
        return null;
    }
}



