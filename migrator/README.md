# Welcome to Bazel Build

Here you will find the info to help you get started [migrating your projects](./docs/) to bazel.

## Step 1: Migrate Phase

Your service needs to pass migrator service on jenkins.  
You might be one of the fortunate ones where migrate passed automatically, if so smile and move on to the next step.

Note: migrator stage is difficult to run locally so check in changes and see that the migration passes on jenkins  
Note: changes in `pom.xml` will only be discovered in _Jenkins_ after a **successful** _Teamcity_ build.  

1. Open jenkins (VPN required): http://ci-jenkins-poc0a.42.wixprod.net/  
   * Look for your project (e.g. `user-activity`)
   * We want to have our `01-migrate` job <font color="green">green</font>, If it's green you are done, go to the next step.
   
2. Check build status:
    * Click on `01-migrate` then under **Build History** click the latest run, then  **Console output**.  
    (For example: http://ci-jenkins-poc0a.42.wixprod.net/job/linguist/job/02-run-bazel/7/console)  
    If you can't see the whole log press on the 'full log' link on the top of the log
  
Follow [Troubleshooting Guide](./docs/troubleshooting-migration-failures.md) for common errors.

For more support join our Slack Channel - [`#bazel-migrate-support`](https://wix.slack.com/channels/bazel-migrate-support)


## Step 2: Make your project pass build

1. Setup Local Environment:
   * Install Bazel locally using `brew tap bazelbuild/tap && brew tap-pin bazelbuild/tap && brew install bazelbuild/tap/bazel && mkdir -p /usr/local/lib/jvm && ln -fs $JAVA_HOME /usr/local/lib/jvm/java-8-latest` on Mac OS or for [other platforms](https://docs.bazel.build/versions/master/install.html)
   * Install the IntelliJ Plugin (2018.1 from inside the IDE, for 2018.2 go to #bazel-support)
2. Check out the migration branch:
   * First `git pull` your project to update branches  
   Look for your project's latest migrate branch (for example: https://github.com/wix-private/linguist/branches, latest branch is `bazel-mig-18`)
   * Checkout branch: `git checkout bazel-mig-18`
   
3. Find the build failure (see **Console output** for your build). Fix using the [Troubleshooting Guide](./docs/troubleshooting-build-failures.md).

4. Compile locally

Build
```
# whole project
bazel build //...

# sub-project
bazel build //path/prefix/...

```

Unit Tests
```
# whole project
bazel test --test_tag_filters=UT,-IT //...

# sub-project
bazel test --test_tag_filters=UT,-IT //subproject/...
```

Integration Tests
```
# whole project
bazel test --test_tag_filters=IT --strategy=TestRunner=standalone --jobs=1 //...

# sub-project
bazel test --test_tag_filters=IT --strategy=TestRunner=standalone --jobs=1 //subproject/...
```

## Step 3: Keep it green!
The success of migration process depends on multiple factors:
- Your code
- Your SNAPSHOT dependencies 
- Your SNAPSHOT parent definitions
- Codota index
- The migrator code (we are constantly changing, trying to make the migrator good for everyone and add features needed for the future. we really try to not break anyone. But it happens)

That's why even if you succeed to pass migration / bazel run / comparison - you want to make sure it's still green

Read [here](./docs/getting-slack-notifications.md) how to get notifications about regression straight to your slack channels

### Create bootstrap deployable
add to your `bazel_migration` directory `post-migration.sh` file, and include the a call to `${commons}/`[`bootstrapify`](https://github.com/wix-private/core-server-build-tools/blob/master/scripts/post-migration-commons/bootstrapify).
you can see an example usage [here](../bazel_migration/post-migration.sh#L10).
Jenkins would automatically add `core-server-build-tools/blob/master/scripts/post-migration-commons` to your commons dir
