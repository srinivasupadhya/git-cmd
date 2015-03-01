package com.tw.go.plugin.model;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GitConfigTest {
    @Test
    public void shouldGetEffectiveUrl() throws Exception {
        assertThat(new GitConfig("/tmp/git-repo", null, null, null, null, null).getEffectiveUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("/tmp/git-repo", "username", "password", null, null, null).getEffectiveUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", null, null, null, null, null).getEffectiveUrl(), is("http://github.com/gocd/gocd"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", "username", "password", null, null, null).getEffectiveUrl(), is("http://username:password@github.com/gocd/gocd"));
        assertThat(new GitConfig("https://github.com/gocd/gocd", "username", "password", null, null, null).getEffectiveUrl(), is("https://username:password@github.com/gocd/gocd"));
    }

    @Test
    public void shouldGetEffectiveBranch() throws Exception {
        assertThat(new GitConfig("url", null, false, false).getEffectiveBranch(), is("master"));
        assertThat(new GitConfig("url", "branch", false, false).getEffectiveBranch(), is("branch"));
    }
}
