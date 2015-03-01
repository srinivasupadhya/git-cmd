package com.tw.go.plugin.git;

import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.cmd.ConsoleResult;
import com.tw.go.plugin.cmd.InMemoryConsumer;
import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.Revision;
import com.tw.go.plugin.util.DateUtils;
import com.tw.go.plugin.util.ListUtil;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitCmdHelper extends GitHelper {
    private static final Pattern GIT_SUBMODULE_STATUS_PATTERN = Pattern.compile("^.[0-9a-fA-F]{40} (.+?)( \\(.+\\))?$");
    private static final Pattern GIT_SUBMODULE_URL_PATTERN = Pattern.compile("^submodule\\.(.+)\\.url (.+)$");
    private static final Pattern GIT_DIFF_TREE_PATTERN = Pattern.compile("^(.)\\s+(.+)$");

    public GitCmdHelper(GitConfig gitConfig, File workingDir) {
        this(gitConfig, workingDir, new ProcessOutputStreamConsumer(new InMemoryConsumer()), new ProcessOutputStreamConsumer(new InMemoryConsumer()));
    }

    public GitCmdHelper(GitConfig gitConfig, File workingDir, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        super(gitConfig, workingDir, stdOut, stdErr);
    }

    @Override
    public String version() {
        CommandLine gitLsRemote = Console.createCommand("version");
        return runAndGetOutput(gitLsRemote).stdOut().get(0);
    }

    @Override
    public void checkConnection() {
        CommandLine gitCmd = Console.createCommand("ls-remote", gitConfig.getUrl());
        runAndGetOutput(gitCmd);
    }

    @Override
    public void cloneRepository() {
        List<String> args = new ArrayList<String>();
        args.add("clone");
        args.add(String.format("--branch=%s", gitConfig.getEffectiveBranch()));
        if (gitConfig.isShallowClone()) {
            args.add("--depth=1");
        }
        args.add(gitConfig.getUrl());
        args.add(workingDir.getAbsolutePath());
        CommandLine gitClone = Console.createCommand(args.toArray(new String[args.size()]));
        Console.runOrBomb(gitClone, null, stdOut, stdErr);
    }

    @Override
    public void checkoutRemoteBranchToLocal() {
        CommandLine gitCmd = Console.createCommand("checkout", "-b", gitConfig.getEffectiveBranch(), "origin/" + gitConfig.getEffectiveBranch());
        runOrBomb(gitCmd);
    }

    @Override
    public String workingRepositoryUrl() {
        CommandLine gitConfig = Console.createCommand("config", "remote.origin.url");
        return runAndGetOutput(gitConfig).stdOut().get(0);
    }

    @Override
    public String getCurrentBranch() {
        CommandLine gitRevParse = Console.createCommand("rev-parse", "--abbrev-ref", "HEAD");
        return runAndGetOutput(gitRevParse).stdOut().get(0);
    }

    @Override
    public int getCommitCount() {
        CommandLine gitCmd = Console.createCommand("rev-list", "HEAD", "--count");
        return Integer.parseInt(runAndGetOutput(gitCmd).stdOut().get(0));
    }

    @Override
    public String currentRevision() {
        CommandLine gitLog = Console.createCommand("log", "-1", "--pretty=format:%H");
        return runAndGetOutput(gitLog).stdOut().get(0);
    }

    @Override
    public List<Revision> getAllRevisions() {
        return gitLog("log", "--date=iso", "--pretty=medium");
    }

    @Override
    public Revision getLatestRevision() {
        return gitLog("log", "-1", "--date=iso", "--pretty=medium").get(0);
    }

    @Override
    public List<Revision> getRevisionsSince(String revision) {
        return gitLog("log", String.format("%s..", revision), "--date=iso", "--pretty=medium");
    }

    private List<Revision> gitLog(String... args) {
        CommandLine gitLog = Console.createCommand(args);
        List<String> gitLogOutput = runAndGetOutput(gitLog).stdOut();

        List<Revision> revisions = new GitModificationParser().parse(gitLogOutput);
        for (Revision revision : revisions) {
            addModifiedFiles(revision);
        }
        return revisions;
    }

    private void addModifiedFiles(Revision revision) {
        List<String> diffTreeOutput = diffTree(revision.getRevision()).stdOut();

        for (String resultLine : diffTreeOutput) {
            // First line is the node
            if (resultLine.equals(revision.getRevision())) {
                continue;
            }

            Matcher m = matchResultLine(resultLine);
            if (!m.find()) {
                throw new RuntimeException(String.format("Unable to parse git-diff-tree output line: %s\nFrom output:\n %s", resultLine, ListUtil.join(diffTreeOutput, "\n")));
            }
            revision.createModifiedFile(m.group(2), parseGitAction(m.group(1).charAt(0)));
        }
    }

    private ConsoleResult diffTree(String node) {
        CommandLine gitCmd = Console.createCommand("diff-tree", "--name-status", "--root", "-r", node);
        return runAndGetOutput(gitCmd);
    }

    private Matcher matchResultLine(String resultLine) {
        return GIT_DIFF_TREE_PATTERN.matcher(resultLine);
    }

    private String parseGitAction(char action) {
        switch (action) {
            case 'A':
                return "added";
            case 'M':
                return "modified";
            case 'D':
                return "deleted";
            default:
                return "unknown";
        }
    }

    // http://www.kernel.org/pub/software/scm/git/docs/git-log.html
    private String modificationTemplate(String separator) {
        return "%cn <%ce>%n%H%n%ai%n%n%s%n%b%n" + separator;
    }

    @Override
    public void pull() {
        CommandLine gitCommit = Console.createCommand("pull");
        runOrBomb(gitCommit);
    }

    @Override
    public void fetch() {
        stdOut.consumeLine("[GIT] Fetching changes");
        CommandLine gitFetch = Console.createCommand("fetch", "origin");
        runOrBomb(gitFetch);
    }

    @Override
    public void resetHard(String revision) {
        stdOut.consumeLine("[GIT] Updating working copy to revision " + revision);
        CommandLine gitResetHard = Console.createCommand("reset", "--hard", revision);
        runOrBomb(gitResetHard);
    }

    @Override
    public void cleanAllUnversionedFiles() {
        stdOut.consumeLine("[GIT] Cleaning all unversioned files in working copy");
        if (isSubmoduleEnabled()) {
            for (Map.Entry<String, String> submoduleFolder : submoduleUrls().entrySet()) {
                cleanUnversionedFiles(new File(workingDir, submoduleFolder.getKey()));
            }
        }
        cleanUnversionedFiles(workingDir);
    }

    private void cleanUnversionedFiles(File workingDir) {
        CommandLine gitClean = Console.createCommand("clean", "-dff");
        Console.runOrBomb(gitClean, workingDir, stdOut, stdErr);
    }

    @Override
    public void gc() {
        stdOut.consumeLine("[GIT] Performing git gc");
        CommandLine gitGc = Console.createCommand("gc", "--auto");
        runOrBomb(gitGc);
    }

    @Override
    public Map<String, String> submoduleUrls() {
        CommandLine gitConfig = Console.createCommand("config", "--get-regexp", "^submodule\\..+\\.url");
        List<String> submoduleList = runAndGetOutput(gitConfig).stdOut();
        Map<String, String> submoduleUrls = new HashMap<String, String>();
        for (String submoduleLine : submoduleList) {
            Matcher m = GIT_SUBMODULE_URL_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                throw new RuntimeException(String.format("Unable to parse git-config output line: %s\nFrom output:\n%s", submoduleLine, ListUtil.join(submoduleList, "\n")));
            }
            submoduleUrls.put(m.group(1), m.group(2));
        }
        return submoduleUrls;
    }

    @Override
    public List<String> submoduleFolders() {
        CommandLine gitCmd = Console.createCommand("submodule", "status");
        return submoduleFolders(runAndGetOutput(gitCmd).stdOut());
    }

    private List<String> submoduleFolders(List<String> submoduleLines) {
        List<String> submoduleFolders = new ArrayList<String>();
        for (String submoduleLine : submoduleLines) {
            Matcher m = GIT_SUBMODULE_STATUS_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                throw new RuntimeException(String.format("Unable to parse git-submodule output line: %s\nFrom output:\n%s", submoduleLine, ListUtil.join(submoduleLines, "\n")));
            }
            submoduleFolders.add(m.group(1));
        }
        return submoduleFolders;
    }

    @Override
    public void printSubmoduleStatus() {
        stdOut.consumeLine("[GIT] Git sub-module status");
        CommandLine gitSubModuleStatus = Console.createCommand("submodule", "status");
        runOrBomb(gitSubModuleStatus);
    }

    @Override
    public void checkoutAllModifiedFilesInSubmodules() {
        stdOut.consumeLine("[GIT] Removing modified files in submodules");
        CommandLine gitSubmoduleCheckout = Console.createCommand("submodule", "foreach", "--recursive", "git", "checkout", ".");
        runOrBomb(gitSubmoduleCheckout);
    }

    @Override
    public int getSubModuleCommitCount(String subModuleFolder) {
        CommandLine gitCmd = Console.createCommand("rev-list", "HEAD", "--count");
        return Integer.parseInt(runAndGetOutput(gitCmd, new File(workingDir, subModuleFolder)).stdOut().get(0));
    }

    @Override
    public void submoduleInit() {
        CommandLine gitSubModuleInit = Console.createCommand("submodule", "init");
        runOrBomb(gitSubModuleInit);
    }

    @Override
    public void submoduleSync() {
        CommandLine gitSubModuleSync = Console.createCommand("submodule", "sync");
        runOrBomb(gitSubModuleSync);

        CommandLine gitSubModuleForEachSync = Console.createCommand("submodule", "foreach", "--recursive", "git", "submodule", "sync");
        runOrBomb(gitSubModuleForEachSync);
    }

    @Override
    public void submoduleUpdate() {
        CommandLine gitSubModuleUpdate = Console.createCommand("submodule", "update");
        runOrBomb(gitSubModuleUpdate);
    }

    @Override
    public void init() {
        CommandLine gitCmd = Console.createCommand("init");
        runOrBomb(gitCmd);
    }

    @Override
    public void add(File fileToAdd) {
        CommandLine gitAdd = Console.createCommand("add", fileToAdd.getName());
        runOrBomb(gitAdd);
    }

    @Override
    public void commit(String message) {
        CommandLine gitCommit = Console.createCommand("commit", "-m", message);
        runOrBomb(gitCommit);
    }

    @Override
    public void commitOnDate(String message, Date commitDate) {
        Map<String, String> env = new HashMap<String, String>();
        env.put("GIT_AUTHOR_DATE", DateUtils.formatRFC822(commitDate));
        CommandLine gitCmd = Console.createCommand("commit", "-m", message);
        // TODO: set env.
        runOrBomb(gitCmd);
    }

    @Override
    public void submoduleAdd(String repoUrl, String submoduleNameToPutInGitSubmodules, String folder) {
        String[] addSubmoduleWithSameNameArgs = new String[]{"submodule", "add", repoUrl, folder};
        runOrBomb(Console.createCommand(addSubmoduleWithSameNameArgs));

        String[] changeSubmoduleNameInGitModules = new String[]{"config", "--file", ".gitmodules", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};
        runOrBomb(Console.createCommand(changeSubmoduleNameInGitModules));

        String[] addGitModules = new String[]{"add", ".gitmodules"};
        runOrBomb(Console.createCommand(addGitModules));
    }

    @Override
    public void removeSubmoduleSectionsFromGitConfig() {
        stdOut.consumeLine("[GIT] Cleaning submodule configurations in .git/config");
        for (String submoduleFolder : submoduleUrls().keySet()) {
            configRemoveSection("submodule." + submoduleFolder);
        }
    }

    @Override
    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);

        CommandLine gitConfig = Console.createCommand("config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName);
        runOrBomb(gitConfig);

        CommandLine gitRm = Console.createCommand("rm", "--cached", folderName);
        runOrBomb(gitRm);

        FileUtils.deleteQuietly(new File(workingDir, folderName));
    }

    private void configRemoveSection(String section) {
        CommandLine gitCmd = Console.createCommand("config", "--remove-section", section);
        runOrBomb(gitCmd);
    }

    @Override
    public void changeSubmoduleUrl(String submoduleName, String newUrl) {
        CommandLine gitConfig = Console.createCommand("config", "--file", ".gitmodules", "submodule." + submoduleName + ".url", newUrl);
        runOrBomb(gitConfig);
    }

    @Override
    public void push() {
        CommandLine gitCommit = Console.createCommand("push");
        runOrBomb(gitCommit);
    }

    private ConsoleResult runOrBomb(CommandLine commandLine) {
        return Console.runOrBomb(commandLine, workingDir, stdOut, stdErr);
    }

    private ConsoleResult runAndGetOutput(CommandLine gitCmd) {
        return Console.runOrBomb(gitCmd, workingDir, new ProcessOutputStreamConsumer(new InMemoryConsumer()), new ProcessOutputStreamConsumer(new InMemoryConsumer()));
    }

    private ConsoleResult runAndGetOutput(CommandLine gitCmd, File workingDir) {
        return Console.runOrBomb(gitCmd, workingDir, new ProcessOutputStreamConsumer(new InMemoryConsumer()), new ProcessOutputStreamConsumer(new InMemoryConsumer()));
    }
}
