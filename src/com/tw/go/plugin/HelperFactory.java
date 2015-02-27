package com.tw.go.plugin;

import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.jgit.JGitHelper;
import com.tw.go.plugin.model.GitConfig;

import java.io.File;

public class HelperFactory {
    public static GitHelper git(GitConfig gitConfig, File workingDirectory, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        GitHelper gitCmd = gitCmd(gitConfig, workingDirectory, stdOut, stdErr);
        if (isAvailable(gitCmd))
            return gitCmd;

        return jGit(gitConfig, workingDirectory, stdOut, stdErr);
    }

    public static GitHelper gitCmd(GitConfig gitConfig, File workingDirectory, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        return new GitCmdHelper(gitConfig, workingDirectory, stdOut, stdErr);
    }

    public static GitHelper jGit(GitConfig gitConfig, File workingDirectory, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        return new JGitHelper(gitConfig, workingDirectory, stdOut, stdErr);
    }

    public static GitHelper git(GitConfig gitConfig, File workingDirectory) {
        GitHelper gitCmd = gitCmd(gitConfig, workingDirectory);
        if (isAvailable(gitCmd))
            return gitCmd;

        return jGit(gitConfig, workingDirectory);
    }

    public static GitHelper gitCmd(GitConfig gitConfig, File workingDirectory) {
        return new GitCmdHelper(gitConfig, workingDirectory);
    }

    public static GitHelper jGit(GitConfig gitConfig, File workingDirectory) {
        return new JGitHelper(gitConfig, workingDirectory);
    }

    private static boolean isAvailable(GitHelper gitCmd) {
        try {
            gitCmd.version(); // make sure git is available
            // TODO: check version > 1.8
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}
