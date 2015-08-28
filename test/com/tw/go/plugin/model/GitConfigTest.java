package com.tw.go.plugin.model;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GitConfigTest {
    @Test
    public void shouldGetEffectiveUrl() throws Exception {
        assertThat(new GitConfig("/tmp/git-repo", null, null, null).getEffectiveUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("/tmp/git-repo", "username", "password", null).getEffectiveUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", null, null, null).getEffectiveUrl(), is("http://github.com/gocd/gocd"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", "username", "password", null).getEffectiveUrl(), is("http://username:password@github.com/gocd/gocd"));
        assertThat(new GitConfig("https://github.com/gocd/gocd", "username", "password", null).getEffectiveUrl(), is("https://username:password@github.com/gocd/gocd"));
    }
    
    @Test
    public void shouldGetEffectiveMaskedUrl() throws Exception {
        assertThat(new GitConfig("/tmp/git-repo", null, null, null).getEffectiveMaskedUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("/tmp/git-repo", "username", "password", null).getEffectiveMaskedUrl(), is("/tmp/git-repo"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", null, null, null).getEffectiveMaskedUrl(), is("http://github.com/gocd/gocd"));
        assertThat(new GitConfig("http://github.com/gocd/gocd", "username", "password", null).getEffectiveMaskedUrl(), is("http://username:*****@github.com/gocd/gocd"));
        assertThat(new GitConfig("https://github.com/gocd/gocd", "username", "password", null).getEffectiveMaskedUrl(), is("https://username:*****@github.com/gocd/gocd"));
    }

    @Test
    public void shouldGetEffectiveBranch() throws Exception {
        assertThat(new GitConfig("url", null, null, null).getEffectiveBranch(), is("master"));
        assertThat(new GitConfig("url", null, null, "branch").getEffectiveBranch(), is("branch"));
    }
}
