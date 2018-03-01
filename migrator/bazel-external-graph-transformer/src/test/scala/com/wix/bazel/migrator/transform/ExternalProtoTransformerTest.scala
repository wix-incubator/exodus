package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven
import com.wixpress.build.maven.MavenMakers.{asCompileDependency, someCoordinates, someProtoCoordinates}
import org.specs2.mutable.SpecificationWithJUnit

class ExternalProtoTransformerTest extends SpecificationWithJUnit {

  "ExternalProtoTransformer" should {


    "add external modules with proto classifier and zip packaging as dependencies to proto targets" in {

      val externalProtoArtifact = asCompileDependency(someProtoCoordinates("external_proto_lib"))
      val repoModule = aModuleWith(externalProtoArtifact)
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        repoModule
      )
      )
      val transformer = new ExternalProtoTransformer(Set(repoModule))

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
      val repoModule = aModuleWith(asCompileDependency(someProtoCoordinates("external_proto_lib")))
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Jvm("internal-jvm-lib", Set.empty, "module-path", Set.empty, CodePurpose.Prod(), null)),
        repoModule))
      val transformer = new ExternalProtoTransformer(Set(repoModule))

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(jvmTarget("internal-jvm-lib", dependencies = beEmpty))
        )
      ))
    }

    "*not* add external modules which don't have both proto classifier and zip packaging as dependencies to proto targets" in {
      val repoModule = aModuleWith(
        asCompileDependency(someCoordinates("external_non_proto_zip").copy(classifier = Some("not-proto"))),
        asCompileDependency(someCoordinates("external_proto_jar").copy(packaging = Some("not-zip"))),
        asCompileDependency(someProtoCoordinates("external_proto_zip")))
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        repoModule
      ))

      val transformer = new ExternalProtoTransformer(Set(repoModule))

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
      val repoModule = aModule("some-module")
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        repoModule
      ))
      val transformer = new ExternalProtoTransformer(Set(repoModule))

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = beEmpty
          ))
        )))
    }

    "add multiple external proto modules to proto targets" in {
      val someProtoArtifact = asCompileDependency(someProtoCoordinates("external_proto1"))
      val otherProtoArtifact = asCompileDependency(someProtoCoordinates("external_proto2"))
      val repoModule = aModuleWith(someProtoArtifact, otherProtoArtifact)
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        repoModule)
      )
      val transformer = new ExternalProtoTransformer(Set(repoModule))

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
      val repoModule = aModuleWith(asCompileDependency(someProtoCoordinates("external_proto")))
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set(Target.Proto("internal-proto-dep", "internal-path", Set.empty)))),
        repoModule)
      )
      val transformer = new ExternalProtoTransformer(Set(repoModule))

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = contain(exactly(
              aTarget(name = "proto"), aTarget(name = "internal-proto-dep")
            ))
          ))
        )))
    }

    "never add internal repo dependency as external proto dependency" in {
      val internalProtoArtifact = someProtoCoordinates("internal-proto-lib")
      val repoModule = aModule(internalProtoArtifact, ModuleDependencies())
      val repoModuleThatDependsOnProto = aModuleWith(asCompileDependency(internalProtoArtifact))
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
        Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
        repoModuleThatDependsOnProto
      )
      )
      val transformer = new ExternalProtoTransformer(Set(repoModule, repoModuleThatDependsOnProto))

      transformer.transform(packages) must contain(exactly(
        aPackage(relativePath = startingWith("module-path"),
          target = a(protoTarget("internal-proto-lib",
            dependencies = not(contain(
              externalTarget(
                name = "proto",
                externalWorkspace = equalTo(internalProtoArtifact.workspaceRuleName))
            ))
          ))
        )
      ))
    }

  }

  private def aModuleWith(directDependency: maven.Dependency*) = aModule("no-care").withDirectDependency(directDependency)

}

