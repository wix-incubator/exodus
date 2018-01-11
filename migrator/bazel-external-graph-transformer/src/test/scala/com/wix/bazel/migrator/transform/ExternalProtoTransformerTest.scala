package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model._
import org.specs2.mutable.SpecificationWithJUnit

class ExternalProtoTransformerTest extends SpecificationWithJUnit {

  "ExternalProtoTransformer" should {

    "add external modules with proto classifier and zip packaging as dependencies to proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
         Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
         aModuleWith(Target.MavenJar("external_proto_lib", "//third_party/some/group/id",
           anExternalModule("external_proto_lib").copy(classifier = Some("proto"), packaging = Some("zip"))))
         )
       )
       val transformer = new ExternalProtoTransformer()

       transformer.transform(packages) must contain(exactly(
         aPackage(relativePath = startingWith("module-path"),
           target = a(protoTarget("internal-proto-lib",
             dependencies = contain(exactly(
               aTarget(name = "external_proto_lib", belongsToPackage = endingWith("some/group/id"))
             ))
           ))
         )
       ))
     }

    "*not* add external proto modules to non-proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
         Set(Target.Jvm("internal-jvm-lib", Set.empty, "module-path", Set.empty, CodePurpose.Prod(), null)),
         aModuleWith(Target.MavenJar("external_proto_lib", "//third_party/some/group/id",
           anExternalModule("external_proto_lib").copy(classifier = Some("proto"), packaging = Some("zip"))))
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
           Target.MavenJar("external_non_proto_zip", "//third_party/some/group/id",
            anExternalModule("external_non_proto_zip").copy(classifier = Some("non-proto"), packaging = Some("zip"))),
           Target.MavenJar("external_proto_jar", "//third_party/some/group/id",
            anExternalModule("external_proto_jar").copy(classifier = Some("proto"), packaging = Some("jar"))),
           Target.MavenJar("external_proto_zip", "//third_party/some/group/id",
            anExternalModule("external_proto_zip").copy(classifier = Some("proto"), packaging = Some("zip")))
         ))
       )
       val transformer = new ExternalProtoTransformer()

       transformer.transform(packages) must contain(exactly(
         aPackage(relativePath = startingWith("module-path"),
           target = a(protoTarget("internal-proto-lib",
             dependencies = contain(exactly(
               aTarget(name = "external_proto_zip")
           ))
         ))
       )))
     }

    "not fail when no external proto dependencies exist" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
         Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
         aModuleWith(Seq.empty[Target.MavenJar]:_*))
       )
       val transformer = new ExternalProtoTransformer()

       transformer.transform(packages) must contain(exactly(
         aPackage(relativePath = startingWith("module-path"),
           target = a(protoTarget("internal-proto-lib",
             dependencies = beEmpty
         ))
       )))
     }

    "add multiple external proto modules to proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
         Set(Target.Proto("internal-proto-lib", "module-path", Set.empty)),
         aModuleWith(
           Target.MavenJar("external_proto1", "//third_party/some/group/id",
            anExternalModule("external_proto1").copy(classifier = Some("proto"), packaging = Some("zip"))),
           Target.MavenJar("external_proto2", "//third_party/some/group/id",
            anExternalModule("external_proto2").copy(classifier = Some("proto"), packaging = Some("zip")))
         ))
       )
       val transformer = new ExternalProtoTransformer()

       transformer.transform(packages) must contain(exactly(
         aPackage(relativePath = startingWith("module-path"),
           target = a(protoTarget("internal-proto-lib",
             dependencies = contain(exactly(
               aTarget(name = "external_proto1"), aTarget(name = "external_proto2")
           ))
         ))
       )))
     }

    "preserve existing internal proto dependencies when adding external proto modules to proto targets" in {
      val packages = Set(Package(relativePathFromMonoRepoRoot = "module-path",
         Set(Target.Proto("internal-proto-lib", "module-path", Set(Target.Proto("internal-proto-dep", "internal-path", Set.empty)))),
         aModuleWith(
           Target.MavenJar("external_proto", "//third_party/some/group/id",
            anExternalModule("external_proto").copy(classifier = Some("proto"), packaging = Some("zip")))
         ))
       )
       val transformer = new ExternalProtoTransformer()

       transformer.transform(packages) must contain(exactly(
         aPackage(relativePath = startingWith("module-path"),
           target = a(protoTarget("internal-proto-lib",
             dependencies = contain(exactly(
               aTarget(name = "external_proto"), aTarget(name = "internal-proto-dep")
           ))
         ))
       )))
     }

  }

  private def aModuleWith(mavenJars: Target.MavenJar*) = {
    aModule("no-care", ModuleDependencies(Map(Scope.PROD_COMPILE -> mavenJars.toSet)))
  }

}

