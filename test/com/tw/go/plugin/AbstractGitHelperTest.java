package com.tw.go.plugin;

import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.Revision;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractGitHelperTest {
    private static final int BUFFER_SIZE = 4096;

    protected File testRepository = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
    protected File simpleGitRepository = new File(System.getProperty("java.io.tmpdir"), "simple-git-repository");
    protected File subModuleGitRepository = new File(System.getProperty("java.io.tmpdir"), "sub-module-git-repository");
    protected File branchGitRepository = new File(System.getProperty("java.io.tmpdir"), "branch-git-repository");

    @Before
    public void setUp() {
        cleanTmpFiles();
    }

    @After
    public void tearDown() {
        cleanTmpFiles();
    }

    private void cleanTmpFiles() {
        FileUtils.deleteQuietly(testRepository);
        FileUtils.deleteQuietly(simpleGitRepository);
        FileUtils.deleteQuietly(subModuleGitRepository);
        FileUtils.deleteQuietly(branchGitRepository);
    }

    protected abstract GitHelper getHelper(GitConfig gitConfig, File workingDir);

    @Test
    public void shouldGetVersion() throws Exception {
        GitHelper git = getHelper(null, null);
        assertThat(git.version(), is(not(nullValue())));
    }

    @Test
    public void shouldCheckConnection() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-1.zip");

        GitHelper gitValidRepository = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), null);
        try {
            gitValidRepository.checkConnection();
        } catch (Throwable t) {
            fail("check connection failed for a valid repository");
        }

        GitHelper gitInValidRepository = getHelper(new GitConfig(new File(System.getProperty("java.io.tmpdir"), "non-existing-repository").getAbsolutePath()), null);
        try {
            gitInValidRepository.checkConnection();
            fail("check connection failed for a valid repository");
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
        }
    }

    @Test
    public void shouldGetRevisionForRepository() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-1.zip");

        GitHelper git = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), testRepository);
        git.cloneOrFetch();

        assertThat(git.workingRepositoryUrl(), is(simpleGitRepository.getAbsolutePath()));
        assertThat(git.getCommitCount(), is(1));
        assertThat(git.currentRevision(), is("012e893acea10b140688d11beaa728e8c60bd9f6"));

        Revision revision = git.getDetailsForRevision("012e893acea10b140688d11beaa728e8c60bd9f6");
        verifyRevision(revision, "012e893acea10b140688d11beaa728e8c60bd9f6", "1", 1422184635000L, asList(new Pair("a.txt", "added")));
    }

    @Test
    public void shouldPollRepository() throws Exception {
        // Checkout & Get LatestRevision
        extractToTmp("/sample-repository/simple-git-repository-1.zip");

        GitHelper git = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), testRepository);
        git.cloneOrFetch();

        assertThat(git.getCurrentBranch(), is("master"));
        assertThat(git.getCommitCount(), is(1));

        Revision revision = git.getLatestRevision();

        verifyRevision(revision, "012e893acea10b140688d11beaa728e8c60bd9f6", "1", 1422184635000L, asList(new Pair("a.txt", "added")));

        // Fetch & Get LatestRevisionsSince
        FileUtils.deleteQuietly(simpleGitRepository.getAbsoluteFile());
        extractToTmp("/sample-repository/simple-git-repository-2.zip");

        git.cloneOrFetch();

        assertThat(git.getCurrentBranch(), is("master"));
        assertThat(git.getCommitCount(), is(3));

        List<Revision> newerRevisions = git.getRevisionsSince("012e893acea10b140688d11beaa728e8c60bd9f6");

        assertThat(newerRevisions.size(), is(2));
        verifyRevision(newerRevisions.get(0), "24ce45d1a1427b643ae859777417bbc9f0d7cec8", "3\ntest multiline\ncomment", 1422189618000L, asList(new Pair("a.txt", "modified"), new Pair("b.txt", "added")));
        verifyRevision(newerRevisions.get(1), "1320a78055558603a2c29d803bbaa50d3542ff50", "2", 1422189545000L, asList(new Pair("a.txt", "modified")));

        // poll again
        git.cloneOrFetch();

        newerRevisions = git.getRevisionsSince("24ce45d1a1427b643ae859777417bbc9f0d7cec8");

        assertThat(newerRevisions.isEmpty(), is(true));
    }

    @Test
    public void shouldCheckoutBranch() throws Exception {
        extractToTmp("/sample-repository/branch-git-repository.zip");

        GitHelper git = getHelper(new GitConfig(branchGitRepository.getAbsolutePath(), null, null, "feature-branch"), testRepository);
        git.cloneOrFetch();

        assertThat(git.getCurrentBranch(), is("feature-branch"));
        assertThat(git.getCommitCount(), is(2));
        assertThat(new File(testRepository, "a.txt").exists(), is(true));
        assertThat(new File(testRepository, "b.txt").exists(), is(true));
    }

    @Test
    public void shouldGetBranchToRevisionMap() throws Exception {
        extractToTmp("/sample-repository/branch-git-repository.zip");

        GitHelper git = getHelper(new GitConfig(branchGitRepository.getAbsolutePath(), null, null, null), testRepository);
        git.cloneOrFetch();

        Map<String, String> branchToRevisionMap = git.getBranchToRevisionMap("refs/remotes/origin/");

        assertThat(branchToRevisionMap.size(), is(2));
        assertThat(branchToRevisionMap.get("master"), is("012e893acea10b140688d11beaa728e8c60bd9f6"));
        assertThat(branchToRevisionMap.get("feature-branch"), is("765e24764ee4f6fc10e4301b4f9528c08ff178d4"));
    }

    @Test
    public void shouldRecursiveSubModuleUpdate() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-1.zip");
        extractToTmp("/sample-repository/sub-module-git-repository.zip");

        GitHelper gitRemote = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), simpleGitRepository);
        gitRemote.submoduleAdd(subModuleGitRepository.getAbsolutePath(), "sub-module", "sub-module");
        gitRemote.commit("add sub-module");

        GitConfig gitConfig = new GitConfig(simpleGitRepository.getAbsolutePath(), null, null, "master", true, false);
        GitHelper gitMain = getHelper(gitConfig, testRepository);
        gitMain.cloneOrFetch();

        assertThat(gitMain.getCommitCount(), is(2));

        assertThat(gitMain.getSubModuleCommitCount("sub-module"), is(2));

        // TODO: add commit to sub-module & main-repo

        // poll again
        gitMain.cloneOrFetch();

        assertThat(gitMain.getCommitCount(), is(2));

        assertThat(gitMain.getSubModuleCommitCount("sub-module"), is(2));
    }

    @Test
    public void shouldWorkWithRepositoriesWithSubModules() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-1.zip");
        extractToTmp("/sample-repository/sub-module-git-repository.zip");

        GitHelper gitRemote = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), simpleGitRepository);
        gitRemote.submoduleAdd(subModuleGitRepository.getAbsolutePath(), "sub-module", "sub-module");
        gitRemote.commit("add sub-module");

        List<String> submoduleFolders = gitRemote.submoduleFolders();
        assertThat(submoduleFolders.size(), is(1));
        assertThat(submoduleFolders.get(0), is("sub-module"));
    }

    @Test
    public void shouldShallowClone() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-2.zip");

        GitHelper git = getHelper(new GitConfig("file://" + simpleGitRepository.getAbsolutePath(), null, null, "master", false, true), testRepository);
        git.cloneOrFetch();

        assertThat(git.getCommitCount(), is(1));

        Revision revision = git.getLatestRevision();

        verifyRevision(revision, "24ce45d1a1427b643ae859777417bbc9f0d7cec8", "3\ntest multiline\ncomment", 1422189618000L, asList(new Pair("a.txt", "added"), new Pair("b.txt", "added")));

        // poll again
        git.cloneOrFetch();

        List<Revision> newerRevisions = git.getRevisionsSince("24ce45d1a1427b643ae859777417bbc9f0d7cec8");

        assertThat(newerRevisions.isEmpty(), is(true));
    }

    @Test
    public void shouldCheckoutToRevision() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-2.zip");

        GitHelper git = getHelper(new GitConfig(simpleGitRepository.getAbsolutePath()), testRepository);
        git.cloneOrFetch();

        git.resetHard("24ce45d1a1427b643ae859777417bbc9f0d7cec8");

        assertThat(new File(testRepository, "a.txt").exists(), is(true));
        assertThat(new File(testRepository, "b.txt").exists(), is(true));

        git.resetHard("1320a78055558603a2c29d803bbaa50d3542ff50");

        assertThat(new File(testRepository, "a.txt").exists(), is(true));
        assertThat(new File(testRepository, "b.txt").exists(), is(false));
    }

    @Test
    public void shouldInitAddCommit() throws Exception {
        testRepository.mkdirs();

        GitHelper git = getHelper(null, testRepository);
        git.init();
        File file = new File(testRepository, "a.txt");
        FileUtils.writeStringToFile(file, "content");
        git.add(file);
        git.commit("comment");

        List<Revision> allRevisions = git.getAllRevisions();
        assertThat(allRevisions.size(), is(1));

        Revision revision = allRevisions.get(0);
        assertThat(revision.getComment(), is("comment"));
        assertThat(revision.getModifiedFiles().size(), is(1));
        assertThat(revision.getModifiedFiles().get(0).getFileName(), is("a.txt"));
        assertThat(revision.getModifiedFiles().get(0).getAction(), is("added"));
    }

    @Ignore
    @Test
    public void shouldWorkWithGithubRepository() {
        GitHelper git = getHelper(new GitConfig("https://github.com/mdaliejaz/samplerepo.git"), testRepository);
        git.cloneOrFetch("+refs/pull/*/merge:refs/gh-merge/remotes/origin/*");

        Map<String, String> branchToRevisionMap = git.getBranchToRevisionMap("refs/gh-merge/remotes/origin/");

        assertThat(branchToRevisionMap.size(), is(1));
        assertThat(branchToRevisionMap.get("1"), is("aabd0f242bd40bfaaa4ce359123b2a2d976077d1"));
    }

    protected void extractToTmp(String zipResourcePath) throws IOException {
        File zipFile = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + ".zip");

        IOUtils.copy(getClass().getResourceAsStream(zipResourcePath), new FileOutputStream(zipFile));

        unzip(zipFile.getAbsolutePath(), System.getProperty("java.io.tmpdir"));

        FileUtils.deleteQuietly(zipFile);
    }

    private void unzip(String zipFilePath, String destinationDirectoryPath) throws IOException {
        File destinationDirectory = new File(destinationDirectoryPath);
        if (!destinationDirectory.exists()) {
            FileUtils.forceMkdir(destinationDirectory);
        }

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {
            String filePath = destinationDirectoryPath + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipInputStream, filePath);
            } else {
                FileUtils.forceMkdir(new File(filePath));
            }

            zipInputStream.closeEntry();
            entry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    private void extractFile(ZipInputStream zipInputStream, String filePath) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesRead = new byte[BUFFER_SIZE];
        int readByteCount = 0;
        while ((readByteCount = zipInputStream.read(bytesRead)) != -1) {
            bufferedOutputStream.write(bytesRead, 0, readByteCount);
        }
        bufferedOutputStream.close();
    }

    private void verifyRevision(Revision revision, String sha, String comment, long timestamp, List<Pair> files) {
        assertThat(revision.getRevision(), is(sha));
        assertThat(revision.getTimestamp().getTime(), is(timestamp));
        assertThat(revision.getComment(), is(comment));
        assertThat(revision.getModifiedFiles().size(), is(files.size()));
        for (int i = 0; i < files.size(); i++) {
            assertThat(revision.getModifiedFiles().get(i).getFileName(), is(files.get(i).a));
            assertThat(revision.getModifiedFiles().get(i).getAction(), is(files.get(i).b));
        }
    }
}
