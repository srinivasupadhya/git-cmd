package com.tw.go.plugin;

import com.tw.go.plugin.model.Revision;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface GitHelper {
    public String version();

    public void checkConnection();

    public void cloneOrFetch();

    public void cloneRepository();

    public void checkoutRemoteBranchToLocal();

    public String workingRepositoryUrl();

    public String getCurrentBranch();

    public int getCommitCount();

    public String currentRevision();

    public List<Revision> getAllRevisions();

    public Revision getLatestRevision();

    public List<Revision> getRevisionsSince(String revision);

    public void pull();

    public void fetch();

    public void resetHard(String revision);

    public void fetchAndResetToHead();

    public void fetchAndReset(String revision);

    public void cleanAllUnversionedFiles();

    public void gc();

    public boolean isSubmoduleEnabled();

    public Map<String, String> submoduleUrls();

    public List<String> submoduleFolders();

    public void printSubmoduleStatus();

    public void checkoutAllModifiedFilesInSubmodules();

    public int getSubModuleCommitCount(String subModuleFolder);

    public void updateSubmoduleWithInit();

    public void submoduleInit();

    public void submoduleSync();

    public void submoduleUpdate();

    public void init();

    public void add(File fileToAdd);

    public void commit(String message);

    public void commitOnDate(String message, Date commitDate);

    public void submoduleAdd(String repoUrl, String submoduleNameToPutInGitSubmodules, String folder);

    public void removeSubmoduleSectionsFromGitConfig();

    public void submoduleRemove(String folderName);

    public void changeSubmoduleUrl(String submoduleName, String newUrl);

    public void push();
}
