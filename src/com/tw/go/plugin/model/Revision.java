package com.tw.go.plugin.model;

import com.tw.go.plugin.util.ListUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Revision {
    private String revision;
    private Date timestamp;
    private String comment;
    private String user;
    private List<ModifiedFile> modifiedFiles;

    public Revision(String revision) {
        this.revision = revision;
    }

    public Revision(String revision, int timestamp, String comment, String user, List<ModifiedFile> modifiedFiles) {
        this.revision = revision;
        this.timestamp = new Date(timestamp);
        this.comment = comment;
        this.user = user;
        this.modifiedFiles = modifiedFiles;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<ModifiedFile> getModifiedFiles() {
        return modifiedFiles;
    }

    public void setModifiedFiles(List<ModifiedFile> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    @Override
    public String toString() {
        return "Revision{" +
                "revision='" + revision + '\'' +
                ", timestamp=" + timestamp +
                ", comment='" + comment + '\'' +
                ", user='" + user + '\'' +
                ", modifiedFiles=" + modifiedFiles +
                '}';
    }

    public final ModifiedFile createModifiedFile(String filename, String action) {
        ModifiedFile file = new ModifiedFile(filename, action);
        if (ListUtil.isEmpty(modifiedFiles)) {
            modifiedFiles = new ArrayList<ModifiedFile>();
        }
        modifiedFiles.add(file);
        return file;
    }
}
