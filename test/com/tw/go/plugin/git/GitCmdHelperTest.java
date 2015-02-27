package com.tw.go.plugin.git;

import com.tw.go.plugin.AbstractGitHelperTest;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;

import java.io.File;

public class GitCmdHelperTest extends AbstractGitHelperTest {
    @Override
    protected GitHelper getHelper(GitConfig gitConfig, File workingDir) {
        return HelperFactory.gitCmd(gitConfig, workingDir);
    }
}
