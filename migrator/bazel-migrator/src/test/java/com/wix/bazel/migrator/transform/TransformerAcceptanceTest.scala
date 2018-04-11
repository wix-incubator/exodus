package com.wix.bazel.migrator.transform

import java.util.InputMismatchException

import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model.Target.TargetDependency
import com.wix.bazel.migrator.model.TestType._
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.bazel.migrator.transform.makers.CodeMaker._
import com.wix.bazel.migrator.transform.makers.DependencyMaker._
import com.wix.bazel.migrator.transform.makers.Repo
import org.specs2.matcher.{AlwaysMatcher, Matcher}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class TransformerAcceptanceTest extends SpecificationWithJUnit {

  abstract class Context extends Scope {
    def repo: Repo

    val dependencyAnalyzer = new FakeDependencyAnalyzer(repo)
    val transformer = new BazelTransformer(dependencyAnalyzer)
  }

  "bazel transformer" should {

    "transform a single file of a single module to a single target in a single package" in new Context {
      def repo = Repo().withCode(
        code(module = aModule(relativePathFromMonoRepoRoot = "/single"), filePath = "com/wix/lib/Code.java")
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        //TODO [Tests] think about maybe replacing startingWithAndEndingWith here with beEqual and use /src/main/java (pro will be easier to understand, con source dir wasn't pushed chronoligcally at the time of the test)
        aPackage(relativePath = startingWithAndEndingWith("/single", "com/wix/lib"),
          target = a(jvmTarget(name = "lib", sources = contain(exactly("")), dependencies = beEmpty)))
      ))
    }

    "transform two files, from different source packages, of a single module to targets in different bazel packages" in new Context {
      def repo = Repo()
        .withCode(code(filePath = "com/wix/lib/Code.java"))
        .withCode(code(filePath = "com/wix/lib2/Code2.java"))

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("/com/wix/lib2"), target = a(jvmTarget(name = "lib2"))),
        aPackage(relativePath = endingWith("/com/wix/lib"), target = a(jvmTarget(name = "lib")))
      ))
    }

    "transform files, from different source packages, with a dependency from one to the others to targets where one depends on the others" in new Context {
      def repo = Repo()
        .withCode(code(filePath = "com/wix/lib/Code.java"))
        .withCode(code(filePath = "com/wix/otherLib/Code.java"))
        .withCode(
          code(filePath = "com/wix/lib2/Code2.java",
            dependencies = List(dependency(filePath = "com/wix/lib/Code.java"), dependency(filePath = "com/wix/otherLib/Code.java")))
        )

      val packages = transformer.transform(repo.modules)

      packages must contain(
        aPackage(relativePath = endingWith("com/wix/lib2"), target = a(jvmTarget(name = "lib2", dependencies = contain(exactly(
          aTargetDependency(name = "lib", belongsToPackage = endingWith("com/wix/lib")),
          aTargetDependency(name = "otherLib", belongsToPackage = endingWith("com/wix/otherLib")))))))
      )
    }

    "transform files with a cyclic dependency between them to one target in a bazel package in the common ancestor" in new Context {
      def repo = {
        val someFilePath = "com/wix/lib/Code.java"
        val otherFilePath = "com/wix/lib2/Code2.java"
        Repo()
          .withCode(code(filePath = someFilePath, dependencies = List(dependency(filePath = otherFilePath))))
          .withCode(code(filePath = otherFilePath, dependencies = List(dependency(filePath = someFilePath))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("/com/wix"),
          target = a(jvmTarget(name = aggregatorOf("lib", "lib2"), sources = contain(exactly("/lib", "/lib2")), dependencies = beEmpty)))
      ))
    }

    "transform files with a cyclic dependency between a source package and its sub to one target in a bazel package in the common ancestor" in new Context {
      def repo = {
        //TODO [Tests] should i extract something like aCyclicRepo(file1, file2)? Currently No
        val someFilePath = "com/wix/lib/Code.java"
        val otherFilePath = "com/wix/lib/sub/Sub.java"
        Repo()
          .withCode(code(filePath = someFilePath, dependencies = List(dependency(filePath = otherFilePath))))
          .withCode(code(filePath = otherFilePath, dependencies = List(dependency(filePath = someFilePath))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("/com/wix/lib"),
          target = a(jvmTarget(name = aggregatorOf(".", "sub"), sources = contain(exactly("", "/sub")), dependencies = beEmpty)))
      ))
    }

    "support cycles of cycles" in new Context {
      def repo = {
        val lib1Sub1 = "com/wix/lib1/sub1/Code.java"
        val lib1Sub2 = "com/wix/lib1/sub2/Code.java"
        val lib2Sub1 = "com/wix/lib2/sub1/Code.java"
        val lib2Sub2 = "com/wix/lib2/sub2/Code.java"
        Repo()
          .withCode(code(filePath = lib1Sub1, dependencies = List(dependency(filePath = lib1Sub2))))
          .withCode(code(filePath = lib1Sub2, dependencies = List(dependency(filePath = lib1Sub1), dependency(filePath = lib2Sub1))))
          .withCode(code(filePath = lib2Sub1, dependencies = List(dependency(filePath = lib2Sub2), dependency(filePath = lib1Sub2))))
          .withCode(code(filePath = lib2Sub2, dependencies = List(dependency(filePath = lib2Sub1))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("/com/wix"),
          target = a(jvmTarget(name = aggregatorOf("lib1/sub1", "lib1/sub2", "lib2/sub1", "lib2/sub2"), sources = contain(exactly("/lib1/sub1", "/lib1/sub2", "/lib2/sub1", "/lib2/sub2")), dependencies = beEmpty))
        )))
    }

    "support multiple targets in the same package" in new Context {
      def repo = {
        val cycle1_node1 = "com/wix/lib1/Code.java"
        val cycle1_node2 = "com/wix/lib2/Code.java"
        val cycle2_node1 = "com/wix/lib3/Code.java"
        val cycle2_node2 = "com/wix/lib4/Code.java"
        Repo()
          .withCode(code(filePath = cycle1_node1, dependencies = List(dependency(filePath = cycle1_node2))))
          .withCode(code(filePath = cycle1_node2, dependencies = List(dependency(filePath = cycle1_node1))))
          .withCode(code(filePath = cycle2_node1, dependencies = List(dependency(filePath = cycle2_node2))))
          .withCode(code(filePath = cycle2_node2, dependencies = List(dependency(filePath = cycle2_node1))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackageWithMultipleTargets(relativePath = endingWith("/com/wix"),
          targets = contain(exactly(
            a(jvmTarget(name = aggregatorOf("lib1", "lib2"), sources = contain(exactly("/lib1", "/lib2")))),
            a(jvmTarget(name = aggregatorOf("lib3", "lib4"), sources = contain(exactly("/lib4", "/lib3"))))
          )))
      ))
    }

    "support multiple modules without related dependencies" in new Context {
      def repo = Repo()
        .withCode(code(module = aModule("/group1-dirs/artifact1-dirs"), filePath = "com/wix/group1/artifact1/Code.java"))
        .withCode(code(module = aModule("/group2-dirs/artifact2-dirs"), filePath = "com/wix/group2/artifact2/Code.java"))

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = startingWithAndEndingWith("/group1-dirs/artifact1-dirs", "com/wix/group1/artifact1"),
          target = a(jvmTarget(name = "artifact1", sources = contain(exactly("")), dependencies = beEmpty))),
        aPackage(relativePath = startingWithAndEndingWith("/group2-dirs/artifact2-dirs", "com/wix/group2/artifact2"),
          target = a(jvmTarget(name = "artifact2", sources = contain(exactly("")), dependencies = beEmpty)))
      ))
    }

    "support dependency between two modules" in new Context {
      def repo = Repo()
        .withCode(code(module = aModule("/group1-dirs/artifact1-dirs"), filePath = "com/wix/group1/artifact1/Code.java"))
        .withCode(code(module = aModule("/group2-dirs/artifact2-dirs"), filePath = "com/wix/group2/artifact2/Code.java",
          dependencies = List(dependency(module = aModule("/group1-dirs/artifact1-dirs"), filePath = "com/wix/group1/artifact1/Code.java"))))

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = startingWithAndEndingWith("/group2-dirs/artifact2-dirs", "com/wix/group2/artifact2"),
          target = a(jvmTarget(name = "artifact2", dependencies = contain(
            aTargetDependency(name = "artifact1", belongsToPackage = startingWithAndEndingWith("/group1-dirs/artifact1-dirs", "com/wix/group1/artifact1")))))),
        aPackage(relativePath = startingWithAndEndingWith("/group1-dirs/artifact1-dirs", "com/wix/group1/artifact1"))
      ))
    }

    "support dependency between a package in one module to a cycle in the other module" in new Context {
      def repo = {
        val module = aModule("/group2-dirs/artifact2-dirs")
        Repo()
          .withCode(code(module = aModule("/group1-dirs/artifact1-dirs"), filePath = "com/wix/group1/artifact1/Code.java",
            dependencies = List(dependency(module = module, filePath = "com/wix/group2/artifact2/Code.java"))))
          .withCode(code(module = module, filePath = "com/wix/group2/artifact2/Code.java",
            dependencies = List(dependency(module = module, filePath = "com/wix/group2/artifact3/Code.java"))))
          .withCode(code(module = module, filePath = "com/wix/group2/artifact3/Code.java",
            dependencies = List(dependency(module = module, filePath = "com/wix/group2/artifact2/Code.java"))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = startingWithAndEndingWith("/group2-dirs/artifact2-dirs", "com/wix/group2"),
          target = a(jvmTarget(name = aggregatorOf("artifact2", "artifact3")))),
        aPackage(relativePath = startingWithAndEndingWith("/group1-dirs/artifact1-dirs", "com/wix/group1/artifact1"),
          target = a(jvmTarget(name = "artifact1", dependencies = contain(
            aTargetDependency(name = aggregatorOf("artifact2", "artifact3"), belongsToPackage = endingWith("group2"))))))
      ))
    }

    "explicitly state it doesn't support composing cycles between modules" in new Context {
      def repo = Repo()
        .withCode(code(module = aModule(relativePathFromMonoRepoRoot = "/oss-lib"),
          filePath = "com/wix/samePackage/Code1.java", dependencies = List(
            dependency(module = aModule(relativePathFromMonoRepoRoot = "/proprietary-lib"), filePath = "com/wix/samePackage/Code2.java")
          )))
        .withCode(code(module = aModule(relativePathFromMonoRepoRoot = "/proprietary-lib"),
          filePath = "com/wix/samePackage/Code2.java", dependencies = List(
            dependency(module = aModule(relativePathFromMonoRepoRoot = "/oss-lib"), filePath = "com/wix/samePackage/Code1.java")
          )))

      transformer.transform(repo.modules) must
        throwAn[IllegalArgumentException]("a cycle between two different modules or two top level source dirs and that isn't supported")
    }

    "reflect the relative source dir in the created package path" in new Context {
      def repo = Repo().withCode(
        code(relativeSourceDirPathFromModuleRoot = "/src/main/scala", filePath = "com/wix/lib/Code.scala")
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = contain("/src/main/scala"))
      ))
    }

    "group code and dependencies from same source dirs to the same target" in new Context {
      def repo = Repo().withCode(
        code(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = "com/wix/lib/Code.scala",
          dependencies = List(dependency(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = "com/wix/lib/Code2.java")))
      ).withCode(
        code(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = "com/wix/lib/Code2.java")
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = contain("src/main/scala"))
      ))
    }

    "retain the relative source dir even after identifying a cycle" in new Context {
      def repo = {
        val someFilePath = "com/wix/lib/Code.java"
        val otherFilePath = "com/wix/lib2/Code2.java"
        Repo()
          .withCode(code(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = someFilePath,
            dependencies = List(dependency(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = otherFilePath))))
          .withCode(code(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = otherFilePath,
            dependencies = List(dependency(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = someFilePath))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = contain("/src/main/scala"),
          target = a(jvmTarget(name = aggregatorOf("lib", "lib2"), sources = contain(exactly("/lib", "/lib2")), dependencies = beEmpty)))
      ))
    }

    "explicitly state it doesn't support composing cycles between source directories" in new Context {
      def repo = Repo()
        .withCode(code(relativeSourceDirPathFromModuleRoot = "src/main/java",
          filePath = "com/wix/samePackage/Code1.java", dependencies = List(
            dependency(relativeSourceDirPathFromModuleRoot = "src/main/scala", filePath = "com/wix/samePackage/Code2.java")
          )))
        .withCode(code(relativeSourceDirPathFromModuleRoot = "src/main/scala",
          filePath = "com/wix/samePackage/Code2.java", dependencies = List(
            dependency(relativeSourceDirPathFromModuleRoot = "src/main/java", filePath = "com/wix/samePackage/Code1.java")
          )))

      transformer.transform(repo.modules) must
        throwAn[IllegalArgumentException]("a cycle between two different modules or two top level source dirs and that isn't supported")
    }

    "clearly format an aggregative target(sorted, uses an 'agg' prefix and separates it from parts)" in new Context {
      def repo = {
        val someFilePath = "com/wix/lib/Code.java"
        val otherFilePath = "com/wix/lib2/Code2.java"
        Repo()
          .withCode(code(filePath = someFilePath, dependencies = List(dependency(filePath = otherFilePath))))
          .withCode(code(filePath = otherFilePath, dependencies = List(dependency(filePath = someFilePath))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(aPackage(target = aTarget(name = "agg=lib+lib2"))))
    }

    "externalize originating source module for jvm targets to allow scope track-back" in new Context {
      def repo = {
        Repo()
          .withCode(
            code(filePath = "com/wix/lib/Code.java",
              module = aModule("some-rel-path", anExternalModule("someGroupId", "someArtifactId", "someVersion"))
            ))
      }

      val packages = transformer.transform(repo.modules)
      packages.flatMap(_.targets) must contain(
        a(jvmTarget(name = "lib", originatingSourceModule = be_===(aModule("some-rel-path", anExternalModule("someGroupId", "someArtifactId", "someVersion"))))
        ))
    }

    "classify jvm targets' purpose according to their relative source folders" in new Context {
      def repo = {
        Repo()
          .withCode(code(relativeSourceDirPathFromModuleRoot = "/src/test/scala", filePath = "com/wix/testLib/Code.java"))
          .withCode(code(relativeSourceDirPathFromModuleRoot = "/src/it/java", filePath = "com/wix/itLib/Code.java"))
          .withCode(code(relativeSourceDirPathFromModuleRoot = "/src/e2e/scala", filePath = "com/wix/e2eLib/Code.java"))
          .withCode(code(relativeSourceDirPathFromModuleRoot = "/src/main/java", filePath = "com/wix/prodLib/Code.java"))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(target = a(jvmTarget(name = "testLib", codePurpose = be_===(CodePurpose.Test()))),
          relativePath = contain("/src/test/scala")),
        aPackage(target = a(jvmTarget(name = "itLib", codePurpose = be_===(CodePurpose.Test()))),
          relativePath = contain("/src/it/java")),
        aPackage(target = a(jvmTarget(name = "e2eLib", codePurpose = be_===(CodePurpose.Test()))),
          relativePath = contain("/src/e2e/scala")),
        aPackage(target = a(jvmTarget(name = "prodLib", codePurpose = be_===(CodePurpose.Prod()))),
          relativePath = contain("/src/main/java"))
      ))
    }

    "externalize types of test found in code whose purpose is test" in new Context {
      def repo = {
        Repo()
          .withCode(testCode("com/wix/unit/CodeTest.java"))
          .withCode(testCode("com/wix/it/CodeIT.java"))
          .withCode(testCode("com/wix/e2e/CodeE2E.java"))
          .withCode(testCode("com/wix/mix/SomeTest.java"))
          .withCode(testCode("com/wix/mix/SomeIT.java"))
          .withCode(testCode("com/wix/testSupport/Support.java"))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(target = a(jvmTarget(name = "unit", codePurpose = test(UT)))),
        aPackage(target = a(jvmTarget(name = "it", codePurpose = test(ITE2E)))),
        aPackage(target = a(jvmTarget(name = "e2e", codePurpose = test(ITE2E)))),
        aPackage(target = a(jvmTarget(name = "mix", codePurpose = test(Mixed)))),
        aPackage(target = a(jvmTarget(name = "testSupport", codePurpose = test(None))))
      ))
    }

    "externalize originating source module for packages to allow supporting runtime dependency" in new Context {
      def repo = {
        Repo()
          .withCode(
            code(filePath = "com/wix/lib/Code.java",
              module = aModule("some-rel-path", anExternalModule("someGroupId", "someArtifactId", "someVersion"))
            ))
      }

      val packages = transformer.transform(repo.modules)
      packages must contain(
        aPackage(originatingSourceModule = be_===(aModule("some-rel-path", anExternalModule("someGroupId", "someArtifactId", "someVersion"))))
      )
    }

    "transform files from resources folders to a `resources` target" in new Context {
      def repo = Repo().withCode(
        code(filePath = "some.xml", relativeSourceDirPathFromModuleRoot = "src/main/resources")
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("src/main/resources"),
          target = a(resourcesTarget(name = "resources", belongsToPackage = endingWith("src/main/resources"))))
      ))
    }

    "support dependencies of resource folders on other code (runtime/reflection needs)" in new Context {
      def repo = Repo()
        .withCode(code(filePath = "com/wix/lib/CodeToBeUsedAtRuntime.java"))
        .withCode(
          code(filePath = "conf/evil-reflection.xml", relativeSourceDirPathFromModuleRoot = "src/it/resources",
            dependencies = List(dependency(filePath = "com/wix/lib/CodeToBeUsedAtRuntime.java")))
        )

      val packages = transformer.transform(repo.modules)

      packages must contain(
        aPackage(relativePath = endingWith("src/it/resources/conf"), target = a(resourcesTarget(name = "resources",
          dependencies = contain(exactly(
            aTarget(name = "lib", belongsToPackage = endingWith("com/wix/lib")))))))
      )
    }

    "externalize whether a dependency of a JVM target is needed for compilation" in new Context {
      def repo = Repo()
        .withCode(code(filePath = "com/wix/lib1/CompileDependency.java"))
        .withCode(code(filePath = "com/wix/lib2/RuntimeDependency.java"))
        .withCode(
          code(filePath = "com/wix/user/Code.java",
            dependencies = List(
              dependency(filePath = "com/wix/lib1/CompileDependency.java", isCompileDependency = true),
              dependency(filePath = "com/wix/lib2/RuntimeDependency.java", isCompileDependency = false)
            )
          )
        )

      val packages = transformer.transform(repo.modules)

      packages must contain(
        aPackage(relativePath = endingWith("com/wix/user"), target = a(jvmTarget(name = "user", dependencies = contain(exactly(
          aTargetDependency(name = "lib1", belongsToPackage = endingWith("com/wix/lib1"), isCompileDependency = beTrue),
          aTargetDependency(name = "lib2", belongsToPackage = endingWith("com/wix/lib2"), isCompileDependency = beFalse))
        ))))
      )
    }

    "allow multiple code instances for same file path while accumulating dependencies" in new Context {
      def repo = Repo()
        .withCode(code(filePath = "com/wix/someLib/Code.java"))
        .withCode(code(filePath = "com/wix/someOtherLib/Code.java"))
        .withCode(
          code(filePath = "com/wix/lib/SameFilePath.java",
            dependencies = List(dependency(filePath = "com/wix/someLib/Code.java")))
        )
        .withCode(
          code(filePath = "com/wix/lib/SameFilePath.java",
            dependencies = List(dependency(filePath = "com/wix/someOtherLib/Code.java")))
        )

      val packages = transformer.transform(repo.modules)

      packages must contain(
        aPackage(relativePath = endingWith("com/wix/lib"), target = a(jvmTarget(name = "lib", dependencies = contain(exactly(
          aTargetDependency(name = "someLib"),
          aTargetDependency(name = "someOtherLib"))))))
      )
    }

    "create a single proto target from all protos in a source dir" in new Context {
      def repo = Repo().withCode(
        code(relativeSourceDirPathFromModuleRoot = "src/main/proto", filePath = "com/wix/lib/Model.proto")
      ).withCode(
        code(relativeSourceDirPathFromModuleRoot = "src/main/proto", filePath = "com/wix/other-lib/Model2.proto")
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("src/main/proto"),
          target = a(protoTarget(name = "proto", belongsToPackage = endingWith("src/main/proto"))))
      ))
    }

    "add dependencies to proto" in new Context {
      def repo = {
        Repo()
          .withCode(someProtoCode(moduleRelativePath = "/proto-root"))
          .withCode(someProtoCodeWithDependency(moduleRelativePath = "/proto-leaf", dependingOnModule = "/proto-root"))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = startingWith("/proto-root"),
          target = a(protoTarget(
            name = "proto",
            dependencies = beEmpty))),
        aPackage(relativePath = startingWith("/proto-leaf"),
          target = a(protoTarget(
            name = "proto",
            dependencies = contain(exactly(
              aTarget(name = "proto", belongsToPackage = startingWith("/proto-root"))))
          )))
      ))
    }

    "ensure sources of sub-packages of a cycle in the root of the source dir are padded correcly (non-empty with slash, empty without)" in new Context {
      def repo = {
        val someFilePath = "lib/Code.java"
        val otherFilePath = "Code2.java"
        Repo()
          .withCode(code(filePath = someFilePath, dependencies = List(dependency(filePath = otherFilePath))))
          .withCode(code(filePath = otherFilePath, dependencies = List(dependency(filePath = someFilePath))))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(
          target = a(jvmTarget(name = aggregatorOf(".", "lib"), sources = contain(exactly("", "/lib")), dependencies = beEmpty)))
      ))
    }

    "classify resources targets' purpose according to their relative source folders" in new Context {
      def repo = {
        Repo()
          .withCode(code(relativeSourceDirPathFromModuleRoot = "/src/test/resources", filePath = "foo.xml"))
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(target = a(resourcesTarget(name = "resources",
          belongsToPackage = endingWith("src/test/resources"),
          codePurpose = be_===(CodePurpose.Test(TestType.None)))))
      ))
    }

    "convert bazel label string to ExternalTarget" in new Context {
      def repo = Repo().withCode(
        code(module = aModule(), filePath = "com/wix/lib/Code.java", externalDependencies = Set("@external_workspace//some/path:target_name"))
      )

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endingWith("com/wix/lib"),
          target = a(
            jvmTarget(
              name = "lib",
              dependencies = contain(exactly(
                aTargetDependencyOn(
                  a(externalTarget(
                    name = "target_name",
                    belongsToPackage = endingWith("some/path"),
                    externalWorkspace = equalTo("external_workspace")
                  )),
                  isCompileDependency = beTrue
                )
              )
              )
            )
          )
        )))
    }


    "collect External Targets from multiple code" in new Context {
      def module = aModule()

      def repo = {
        Repo()
          .withCode(
            code(module = module, filePath = "com/wix/lib/Code.java", externalDependencies = Set("@external_workspace//some/path:target_name"))
          )
          .withCode(
            code(module = module, filePath = "com/wix/lib/OtherCode.java", externalDependencies = Set("@other_workspace//some/path:other_target"))
          )
      }

      val packages = transformer.transform(repo.modules)

      packages must contain(exactly(
        aPackage(relativePath = endWith("com/wix/lib"),
          target = a(
            jvmTarget(
              name = "lib",
              dependencies = contain(exactly(
                aTargetDependencyOn(
                  a(externalTarget(
                    name = "target_name",
                    belongsToPackage = endingWith("some/path"),
                    externalWorkspace = equalTo("external_workspace")
                  )),
                  isCompileDependency = beTrue
                ),
                aTargetDependencyOn(
                  a(externalTarget(
                    name = "other_target",
                    belongsToPackage = endingWith("some/path"),
                    externalWorkspace = equalTo("other_workspace")
                  )),
                  isCompileDependency = beTrue
                )
              )
              )
            )
          )
        )))
    }

    "throw exception when given code with invalid bazel label as external dependencies" in new Context {
      def repo = Repo().withCode(
        code(module = aModule(), filePath = "com/wix/lib/Code.java", externalDependencies = Set("some invalid label"))
      )

      transformer.transform(repo.modules) must throwA[RuntimeException]
    }


    //two different types, compile wins?
    //cycles?
  }

  private def someProtoCode(moduleRelativePath: String) =
    code(module = aModule(relativePathFromMonoRepoRoot = moduleRelativePath),
      relativeSourceDirPathFromModuleRoot = "src/main/proto", filePath = "Blah.proto")

  private def someProtoCodeWithDependency(moduleRelativePath: String, dependingOnModule: String) =
    someProtoCode(moduleRelativePath).copy(dependencies =
      List(dependency(module = aModule(dependingOnModule),
        relativeSourceDirPathFromModuleRoot = "src/main/proto",
        filePath = "Blah.proto")))

  //TODO [Tests] see if i can come up with a better name that reflects that here we only care about the module rel path and package path and *don't* care about the source dir rel path
  //maybe 'matchRegardlessOfSourceDirPath' ?
  def startingWithAndEndingWith(modulePath: String, packagePath: String): Matcher[String] = {
    startingWith(modulePath) and endingWith(packagePath)
  }

  def aTargetDependency(name: String,
                        belongsToPackage: Matcher[String] = AlwaysMatcher[String](),
                        isCompileDependency: Matcher[Boolean] = AlwaysMatcher[Boolean]()
                       ): Matcher[TargetDependency] =
    aTarget(name, belongsToPackage) ^^ {
      (_: TargetDependency).target aka "target"
    } and isCompileDependency ^^ {
      (_: TargetDependency).isCompileDependency aka "is compile dependency"
    }

  def aTargetDependencyOn(target: Matcher[Target] = AlwaysMatcher[Target](),
                          isCompileDependency: Matcher[Boolean] = AlwaysMatcher[Boolean]()
                         ): Matcher[TargetDependency] =
    target ^^ {
      (_: TargetDependency).target aka "target"
    } and isCompileDependency ^^ {
      (_: TargetDependency).isCompileDependency aka "is compile dependency"
    }

  def resourcesTarget(name: String,
                      belongsToPackage: Matcher[String] = AlwaysMatcher[String](),
                      dependencies: Matcher[Set[Target]] = AlwaysMatcher[Set[Target]](),
                      codePurpose: Matcher[CodePurpose] = AlwaysMatcher[CodePurpose]()
                     ): Matcher[Target.Resources] =
    aTarget(name, belongsToPackage) and
      dependencies ^^ {
        (_: Target.Resources).dependencies aka "dependencies"
      } and codePurpose ^^ {
      (_: Target.Resources).codePurpose aka "code purpose"
    }

  def aggregatorOf(targets: String*): String = "agg=" + targets.mkString("+")

  def test(testType: TestType): Matcher[CodePurpose] = be_===(CodePurpose.Test(testType))
}





//TODO add test (maybe scala check) to check ResourceKey.commonPrefix mainly so that we will be comfortable knowing all edge cases are handled. for example com/wix/lib/sub and com/wix/lib2/sub2
