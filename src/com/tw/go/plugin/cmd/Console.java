package com.tw.go.plugin.cmd;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;

public class Console {
    public static CommandLine createCommand(String... args) {
        CommandLine gitCmd = new CommandLine("git");
        gitCmd.addArguments(args);
        return gitCmd;
    }
    
    public static ConsoleResult runOrBomb(CommandLine commandLine, File workingDir, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr) {
        return runOrBomb(commandLine, workingDir, stdOut, stdErr, commandLine.toString());
    }

    public static ConsoleResult runOrBomb(CommandLine commandLine, File workingDir, ProcessOutputStreamConsumer stdOut, ProcessOutputStreamConsumer stdErr, String prettyMessage) {
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(stdOut, stdErr));
        if (workingDir != null) {
            executor.setWorkingDirectory(workingDir);
        }

        try {
            int exitCode = executor.execute(commandLine);

            if (exitCode != 0) {
                throw new RuntimeException(getMessage("Error", prettyMessage, workingDir));
            }

            return new ConsoleResult(exitCode, stdOut.output(), stdErr.output());
        } catch (Exception e) {
            throw new RuntimeException(getMessage("Exception", prettyMessage, workingDir), e);
        }
    }

    private static String getMessage(String type, String prettyMessage, File workingDir) {
        return String.format("%s Occurred: %s - %s", type, prettyMessage, workingDir);
    }
}
