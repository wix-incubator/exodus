package com.wixpress.build.maven

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.XmlMatchers._

import scala.xml._

class ArtifactDescriptorTest extends SpecificationWithJUnit {
  val baseCoordiantes = Coordinates("some.group", "some-artifact", "some-version")
  val baseDescriptor = ArtifactDescriptor.anArtifact(baseCoordiantes)
  val depCoordinates = Coordinates("some.group", "some-dep", "other-version")
  val dependency = Dependency(depCoordinates, MavenScope.Compile)

  "ArtifactDescriptor" should {
    "reconstruct itself with new  dependency" in {
      baseDescriptor.withDependency(dependency).dependencies must contain(dependency)
    }

    "reconstruct itself with new managed dependency" in {
      baseDescriptor.withManagedDependency(dependency).managedDependencies must contain(dependency)
    }

    "build pom.xml" in {
      val expected: Elem = <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
                                    xmlns="http://maven.apache.org/POM/4.0.0"
                                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>{baseCoordiantes.groupId}</groupId>
                            <artifactId>{baseCoordiantes.artifactId}</artifactId>
                            <version>{baseCoordiantes.version}</version>
                          </project>

      val pom: Elem = XML.loadString(baseDescriptor.pomXml)

      pom must beEqualToIgnoringSpace(expected)
    }

    "build pom.xml with dependency and exclusions" in {
      val exclusion = Exclusion("excluded.group","excluded-artifact")
      val dependencyWithExclusions = dependency.copy(exclusions=Set(exclusion))
      val expected = <dependency>
        <groupId>{dependencyWithExclusions.coordinates.groupId}</groupId>
        <artifactId>{dependencyWithExclusions.coordinates.artifactId}</artifactId>
        <version>{dependencyWithExclusions.coordinates.version}</version>
        <scope>{dependencyWithExclusions.scope.name}</scope>
        <exclusions>
          <exclusion>
            <artifactId>{exclusion.artifactId}</artifactId>
            <groupId>{exclusion.groupId}</groupId>
          </exclusion>
        </exclusions>
      </dependency>
      val pom: Elem = XML.loadString(baseDescriptor.withDependency(dependencyWithExclusions).pomXml)
      val allDeps = pom \ "dependencies" \ "dependency"

      allDeps aka "dependencies in generated pom" must not be empty
      allDeps.head must beEqualToIgnoringSpace(expected)
    }
  }
}
