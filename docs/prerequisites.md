# Assumptions
The migration tool assumes you have:
* Java and/or Scala Git repositority currently being built by Maven. 

# Prerequisites
* Install Bazel https://docs.bazel.build/versions/master/install.html

## Code Analysis
Maven runs builds of coarse-grained code units ("modules") which is partly what slows down the build process. Bazel is able to run builds of finer-grained code units ("packages") incrementally and in parallel which makes it much faster. To fully take advantage of Bazel's speed and efficiency, your Maven code units have to be broken down into smaller packages.

Exodus creates these smaller packages using a dependency analysis index. But first you have to use a tool to analyze the code to determine all the dependencies between the code units. 

These are the avialble tools we have so far. As this is an open source project, we'd be happy to hear from you regarding any other options.

### Scala-Maven-Plugin with Zinc (open source path)
Exodus can use Zinc's dependency analysis output. All you have to do is configure a [Scala-Maven-plugin](http://davidb.github.io/scala-maven-plugin/index.html) to use incremental mode with Zinc as the [incremental compiler](http://davidb.github.io/scala-maven-plugin/example_incremental.html). Zinc provides dependency files as its output.

The Scala-Maven plugin version has to correlate with the Maven version you've been using to build the project. 
Here is an example of the plugin version to add:
```xml
 <build>
     <pluginManagement>	       
         <plugins>	            
             <plugin>
		<groupId>net.alchim31.maven</groupId>
		<artifactId>scala-maven-plugin</artifactId>
		<version>3.2.2</version>
	        <configuration>
	  	   <recompileMode>incremental</recompileMode>
	        </configuration>
	     </plugin>
	 </plugins>
     </pluginManagement>
				
```
You also have to add the Scala library dependency and that version also depends on the Maven version you are using.
Here is an example of the dependency code for the Scala library:
```
<dependencyManagement>
     <dependencies>
    	<dependency>
		<groupId>org.scala-lang</groupId>
		<artifactId>scala-library</artifactId>
		<version>2.12.6</version>
	</dependency>
...
    </dependencies>
 </dependencyManagement>

```

### Codota (paid path)
For a part of our migration at Wix, we used a licensed product called Codota. The advantage of using Codota is that you get full support from their team and can have them help you with any special edge cases you may have. The disadvantage is that it's not open source and must be licensed.
