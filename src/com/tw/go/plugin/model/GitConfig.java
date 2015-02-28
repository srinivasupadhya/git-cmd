package com.tw.go.plugin.model;

import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.util.StringUtil;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;

public class GitConfig {
    private String url;
    private String username;
    private String password;
    private String branch;
    private boolean subModule = false;
    private boolean recursiveSubModuleUpdate = false;
    private boolean shallowClone = false;

    public GitConfig(String url) {
        this.url = url;
    }

    public GitConfig(String url, String branch, boolean recursiveSubModuleUpdate, boolean shallowClone) {
        this.url = url;
        this.branch = branch;
        this.recursiveSubModuleUpdate = recursiveSubModuleUpdate;
        this.shallowClone = shallowClone;
    }

    public GitConfig(String url, String username, String password, String branch, String recursiveSubModuleUpdate, String shallowClone) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.branch = branch;
        this.recursiveSubModuleUpdate = StringUtil.isEmpty(recursiveSubModuleUpdate) ? false : Boolean.parseBoolean(recursiveSubModuleUpdate);
        this.shallowClone = StringUtil.isEmpty(shallowClone) ? false : Boolean.parseBoolean(shallowClone);
    }

    public boolean isRemoteUrl() {
        return url.startsWith("http") || url.startsWith("https");
    }

    public boolean hasCredentials() {
        return !StringUtil.isEmpty(url) && !StringUtil.isEmpty(password);
    }

    public void validateUrl(Map<String, Object> fieldMap) {
        if (StringUtil.isEmpty(url)) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "URL is a required field");
        } else {
            if (url.startsWith("/")) {
                if (!new File(url).exists()) {
                    fieldMap.put("key", "url");
                    fieldMap.put("message", "Invalid URL. Directory does not exist");
                }
            } else {
                if (!new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(url)) {
                    fieldMap.put("key", "url");
                    fieldMap.put("message", "Invalid URL format");
                }
            }
        }
    }

    public void checkConnection(Map<String, Object> response, ArrayList<String> messages) {
        try {
            if (StringUtil.isEmpty(url)) {
                response.put("status", "failure");
                messages.add("URL is empty");
            } else if (url.startsWith("/")) {
                if (!new File(url).exists()) {
                    response.put("status", "failure");
                    messages.add("Could not find Git repository");
                } else {
                    HelperFactory.git(this, null).checkConnection();
                }
            } else {
                HelperFactory.git(this, null).checkConnection();
            }
        } catch (Exception e) {
            response.put("status", "failure");
            messages.add(e.getMessage());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEffectiveBranch() {
        return StringUtil.isEmpty(branch) ? "master" : branch;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public boolean isSubModule() {
        return subModule;
    }

    public void setSubModule(boolean subModule) {
        this.subModule = subModule;
    }

    public boolean isRecursiveSubModuleUpdate() {
        return recursiveSubModuleUpdate;
    }

    public void setRecursiveSubModuleUpdate(boolean recursiveSubModuleUpdate) {
        this.recursiveSubModuleUpdate = recursiveSubModuleUpdate;
    }

    public boolean isShallowClone() {
        return shallowClone;
    }

    public void setShallowClone(boolean shallowClone) {
        this.shallowClone = shallowClone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitConfig gitConfig = (GitConfig) o;

        if (recursiveSubModuleUpdate != gitConfig.recursiveSubModuleUpdate) return false;
        if (shallowClone != gitConfig.shallowClone) return false;
        if (branch != null ? !branch.equals(gitConfig.branch) : gitConfig.branch != null) return false;
        if (password != null ? !password.equals(gitConfig.password) : gitConfig.password != null) return false;
        if (url != null ? !url.equals(gitConfig.url) : gitConfig.url != null) return false;
        if (username != null ? !username.equals(gitConfig.username) : gitConfig.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (recursiveSubModuleUpdate ? 1 : 0);
        result = 31 * result + (shallowClone ? 1 : 0);
        return result;
    }
}
