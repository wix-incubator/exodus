package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.transform.makers.CodePathMaker.sourceCodePath
import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import org.specs2.matcher.TypedEqual
import org.specs2.mutable.SpecWithJUnit

class SourceResourceKeyTest extends SpecWithJUnit with TypedEqual {

  "a Source resource key" should {

    "support construction from a file in the root of a source folder of a module" in {
      val codePath = sourceCodePath("SomeFile.java", aModule("some-module"), "source/folder")

      val resourceKey = ResourceKey.fromCodePath(codePath)

      //should not end with slash since that is not a legal package name in bazel
      //and will cause errors when will be used as a dependency
      resourceKey.packageRelativePath ==== "some-module/source/folder"
    }

    "support construction from a file in a single-module project which is itself the root" in {
      val codePath = sourceCodePath("com/SomeFile.java", aModule(""), "src/main/java")

      val resourceKey = ResourceKey.fromCodePath(codePath)

      //should not start with slash since that is not a legal package name in bazel
      //and will cause errors when will be used as a dependency
      resourceKey.packageRelativePath ==== "src/main/java/com"
    }

  }
}
