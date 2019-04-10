# trouble shooting compare failures

We collect all the tests that maven ran and compare it against the tests that ran in bazel.

If maven failed to run - we cannot do the compare.


### Issue reading assembly descriptor
If you get something like this in build log:
```
 Reading assembly descriptor: maven/assembly/tar.gz.xml
 ...
  Failed to execute goal org.apache.maven.plugins:maven-assembly-plugin:2.2.1:single (default) on project cashier-merchant-settings: Failed to create assembly: Error creating assembly archive wix-angular: You must set at least one file.
```
A very common reason for maven failure is npm modules that have pom.
In regular CI, we run `npm build`, and then `mvn install` only to create package using `maven-assembly-plugin`. This run would fail if `npm build` did not run just before it.
In the migration server , we simple don't run `npm build` and that's why maven would fail on missing descriptor file.

Simple solution for that is to add `shouldSkipAssembly` property to that pom with default value of `false`.

On the migration server we run with override property `-DshouldSkipAssembly=true`.

Should look like this:
```xml
    <properties>
        <shouldSkipAssembly>false</shouldSkipAssembly>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>2.2.1</version>
                <configuration>
                    <descriptors>
                        <descriptor>maven/assembly/tar.gz.xml</descriptor>
                    </descriptors>
                    <skipAssembly>${shouldSkipAssembly}</skipAssembly>
                    <appendAssemblyId>false</appendAssemblyId>
                    <finalName>${project.artifactId}-${project.version}</finalName>
                </configuration>
                <executions>
                    <execution>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
``` 

### Issue reading different test number for Maven and Bazel
If the number of total tests in Bazel and Maven is different - check if you use `Fragments.foreach` (or any other kind of loops to generate the names of test cases) in the files that have differences.

Maven does not generate names for tests with `Fragments` correctly - it will produce duplicated test names in the report. The duplications are ignored by the compare process, and that's why you may get a lower number of Maven tests compared to Bazel. 

A way to fix this would be to either:
  1. Not use `Fragments` - if possible, rewrite the test names to use static names instead, or
  2. (after verifying tests are passing in Bazel) - mute the problematic test suites by adding them to `muted_for_maven_comparison.overrides` file in your `bazel_migration` directory.
