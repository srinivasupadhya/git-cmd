Common module that all Go CD plugins to poll Git repository can use.

*Usage:*
Inside `git-cmd` project:
```
$ mvn clean install -DskipTests
```

Add Dependency (to plugin project):
```
<dependency>
    <groupId>com.thoughtworks.go</groupId>
    <artifactId>git-cmd</artifactId>
    <version>0.1</version>
</dependency>
```

Use:
```
GitHelper git = HelperFactory.git(gitConfig, new File(flyweightFolder));
git.cloneOrFetch();
...
```

`HelperFactory.git(gitConfig, new File(flyweightFolder));` detects & uses git if installed else falls back on jgit implementation.