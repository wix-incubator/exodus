package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.CodePurpose.{Prod, Test}
import com.wix.bazel.migrator.model.Target.{Jvm, ModuleDeps, TargetDependency}
import com.wix.bazel.migrator.model.TestType.UT
import com.wix.bazel.migrator.model.{ModuleDependencies, Package, SourceModule}
import com.wixpress.build.maven.MavenMakers.{randomCoordinates, someCoordinates}
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope, Packaging}

class WriterIT extends BaseWriterIT {
  "Writer" should {
    "write Junit5 tests with correct test_package if containing module depends on `jupiter`" in  new ctx {
      val moduleWithJupiter: SourceModule = module.copy(dependencies = dependenciesIncludingJunitJupiter)
      val writer = writerForModule(moduleWithJupiter)

      writer.write()
      path(s"$moduleName/$testSourcePath/$packagePath/BUILD.bazel")  must beRegularFileWithPartialContent(withContentContaining = Seq(
        "java_junit5_test(",
        """test_package = "com.package_a","""
      ))
    }

    "write specs2 tests for modules with no 'jupiter' dependency" in  new ctx {
      val writer = writerForModule(module)

      writer.write()
      path(s"$moduleName/$testSourcePath/$packagePath/BUILD.bazel")  must beRegularFileWithPartialContent(withContentContaining = Seq(
        "specs2_unit_test(",
        s"""jvm_flags = ["-Dexisting.manifest=$$(location //$moduleName:coordinates)"],"""
      ))
    }
  }

  abstract class ctx extends baseCtx {
    val dependenciesIncludingJunitJupiter = ModuleDependencies(directDependencies = Set(Dependency(randomCoordinates().copy(groupId = "org.junit.jupiter"), MavenScope.Test, false, Set())))

    val moduleName = "module-a"
    val module = SourceModule(moduleName,someCoordinates(moduleName))

    val targetPackageName = "package_a"
    val testSourcePath = "src/test/java"
    val prodSourcePath = "src/main/java"
    val packagePath = s"com/$targetPackageName"

    // TODO: SourceModuleMakers...
    // 1 prod package and 1 test package...
    val testPackage = Package(s"$moduleName/$testSourcePath/$packagePath",
      targets = Set(
        Jvm(moduleName, Set(), s"$moduleName/$testSourcePath/$packagePath",
          Set(TargetDependency(Jvm(targetPackageName, Set(""), s"$moduleName/$prodSourcePath/$packagePath", Set(), Prod(), module), isCompileDependency = true)),
          Test(UT),
          module)),
      module)

    val prodPackage = Package(s"$moduleName/$prodSourcePath/$packagePath",
      targets = Set(
        Jvm(moduleName, Set(), s"$moduleName/$prodSourcePath/$packagePath",
          Set(),
          Prod(),
          module)),
      module)

    val modulePackage = Package(moduleName,
      targets = Set(
        ModuleDeps("main_dependencies",moduleName, Set(), Set(), testOnly = false, originatingSourceModule = module),
        ModuleDeps("test_dependencies",moduleName, Set(), Set(), testOnly = true, originatingSourceModule = module)
      ),
      module)

    def packagesFor(module: SourceModule): Set[Package] = {
      val strippedModule = module.copy(dependencies = ModuleDependencies())
      Set(prodPackage, testPackage, modulePackage).map(p => p.copy(originatingSourceModule = strippedModule, targets = p.targets.map {
        case j: Jvm => j.copy(originatingSourceModule = strippedModule)
        case m: ModuleDeps => m.copy(originatingSourceModule = strippedModule)
        case t => t
      }))
    }

    def writerForModule(module: SourceModule) = {
      new Writer(repoRoot, repoModules = Set(module), bazelPackages = packagesFor(module))
    }

  }
}
