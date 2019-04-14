# Prerequisites to consider

The migration tool assumes you have the following:
* Java and or Scala GitHub repositority currently being build by Maven 
* Bazel 

With the Exodus tool you can either:
* [Run migration locally](how-to-run-bazel-locally.md) 
* [Run the migration on Jenkins](how-to-run-migration-jenkins.md)

## Code Analysis
Maven runs builds in large packages which is partly what slows down the build process. A build run's one POM file determines the dependencies for the whole build run. Bazel is able to run builds in smaller packages of code incrementally and in parallel which makes it much faster. But to fully take advantage of Bazel's speed and efficiency, your code has to be broken down into smaller packages. You 

You have to use a tool to analyze the code to determine all the dependencies between packages. Different code bases will need different tweaks.

These are the options we've discovered so far. As this is an open source project, we'd be happy to hear from you regarding any other options.

### Codota (paid path)
At Wix, we used a licensed product called Codota. The advantage of using Codota is that you get full support from their team and can ______. The disadvantage is that that it's not open source and must be licensed.

### Scala-Maven-Plugin with Zinc (open source path)
We want this migration tool to be completely open source, so we are working on an alternative to Codota using a [Scala-Maven-plugin](http://davidb.github.io/scala-maven-plugin/index.html) used in incremental mode with Zinc as the [incremental compiler](http://davidb.github.io/scala-maven-plugin/example_incremental.html). Zinc provides dependency files as its output.

The Scala-Maven plugin version has to correlate with the Maven version you've been using to build the project. 
Here is an example of the plugin version to add:
```
 <build>
        <pluginManagement>	        <pluginManagement>
            <plugins>	            <plugins>
            	<plugin>
					<groupId>net.alchim31.maven</groupId>
					<artifactId>scala-maven-plugin</artifactId>
					<version>3.2.2</version>
                    <configuration>
                        <recompileMode>incremental</recompileMode>
                    </configuration>
				</plugin>
```
You also have to add Scala library dependency and that version also depends on Maven version.
Here is an example of the dependency code for the Scala library:
```
</dependencyManagement>
     <dependencies>	    <dependencies>
    	<dependency>
			<groupId>org.scala-lang</groupId>
			<artifactId>scala-library</artifactId>
			<version>2.12.6</version>
		</dependency>
        <dependency>	        <dependency>
            <groupId>org.slf4j</groupId>	
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>	 
            <artifactId>slf4j-api</artifactId>
```
