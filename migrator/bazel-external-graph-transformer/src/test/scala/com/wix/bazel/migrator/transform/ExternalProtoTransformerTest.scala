package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.MavenMakers.asCompileDependency
import com.wixpress.build.maven.{Dependency, MavenScope}
import org.specs2.mutable.SpecificationWithJUnit
import com.wixpress.build.maven

class ExternalProtoTransformerTest extends SpecificationWithJUnit {

  "ExternalProtoTransformer" should {


    "add external modules with proto classifier and zip packaging as dependencies to proto targets" in {
      val externalProtoArtifact = Dependency(anExternalModule("external_proto_lib").copy(classifier = Some("proto"), packaging = Some("zip")), MavenScope.Compile)
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        aModuleWith(externalProtoArtifact)
      )
      )
      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = contain(exactly(
              externalTarget(
                name = "proto",
                belongsToPackage = equalTo(""),
                externalWorkspace = equalTo(externalProtoArtifact.coordinates.workspaceRuleName))
            ))
          ))
        )
      ))
    }

    "*not* add external proto modules to non-proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Jvm("internal-jvm-lib", Set.empty, "module-path", Set.empty, CodePurpose.Prod(), null)),
        aModuleWith(
          Dependency(anExternalModule("external_proto_lib").copy(classifier = Some("proto"), packaging = Some("zip")), MavenScope.Compile))
      )
      )
      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(jvmTarget("internal-jvm-lib", dependencies = beEmpty))
        )
      ))
    }

    "*not* add external modules which don't have both proto classifier and zip packaging as dependencies to proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        aModuleWith(
          asCompileDependency(anExternalModule("external_non_proto_zip").copy(classifier = Some("non-proto"), packaging = Some("zip"))),
          asCompileDependency(anExternalModule("external_proto_jar").copy(classifier = Some("proto"), packaging = Some("jar"))),
          asCompileDependency(anExternalModule("external_proto_zip").copy(classifier = Some("proto"), packaging = Some("zip"))))
      ))

      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = contain(exactly(
              aTarget(name = "proto")
            ))
          ))
        )))
    }

    "not fail when no external proto dependencies exist" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        aModule("some-module")
      ))
      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = beEmpty
          ))
        )))
    }

    "add multiple external proto modules to proto targets" in {
      val someProtoArtifact = Dependency(anExternalModule("external_proto1").copy(classifier = Some("proto"), packaging = Some("zip")), MavenScope.Compile)
      val otherProtoArtifact = Dependency(anExternalModule("external_proto2").copy(classifier = Some("proto"), packaging = Some("zip")), MavenScope.Compile)
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        aModuleWith(someProtoArtifact, otherProtoArtifact))
      )
      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = contain(exactly(
              externalTarget(name = "proto", externalWorkspace = equalTo(someProtoArtifact.coordinates.workspaceRuleName)),
              externalTarget(name = "proto", externalWorkspace = equalTo(otherProtoArtifact.coordinates.workspaceRuleName))
            ))
          ))
        )))
    }

    "preserve existing internal proto dependencies when adding external proto modules to proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set(Target.Proto("internal-proto-dep", "internal-path", Set.empty)))),
        aModuleWith(asCompileDependency(anExternalModule("external_proto").copy(classifier = Some("proto"), packaging = Some("zip")))))
      )
      val transformer = new ExternalProtoTransformer()

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = contain(exactly(
              aTarget(name = "proto"), aTarget(name = "internal-proto-dep")
            ))
          ))
        )))
    }

  }

  private def aModuleWith(directDependency: maven.Dependency*) = aModule("no-care").withDirectDependency(directDependency)

}

