package com.tw.go.plugin;

import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class HelperFactoryTest {
    @Test
    public void shouldGiveGitCmdIfAvailable() {
        GitHelper git = HelperFactory.git(new GitConfig("url"), null);
        assertThat(git, instanceOf(GitCmdHelper.class));
    }
}
