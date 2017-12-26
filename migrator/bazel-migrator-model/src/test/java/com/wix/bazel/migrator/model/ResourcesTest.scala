package com.wix.bazel.migrator.model

import com.wix.bazel.migrator.model.ResourcesTest._
import com.wix.bazel.migrator.model.Target.Resources
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.core.Fragments

class ResourcesTest extends SpecificationWithJUnit {

  "Resources.applicablePackage" should {
    Fragments.foreach(packageRelativePathToApplicability) { case (packageRelativePath, isApplicable) =>
      s"return ${asString(isApplicable)} ($isApplicable) for $packageRelativePath" in {
        Resources.applicablePackage(packageRelativePath) ==== isApplicable
      }
    }

  }
  val packageRelativePathToApplicability = Seq(
    "/src/main/resources/" -> Applicable,
    "/src/main/java/" -> NotApplicable,
    "/src/foo/resources/" -> Applicable,
    "/src/main/java/com/wix/resources/"  -> NotApplicable
  ).map(prependModulePrefix)

  private def prependModulePrefix(path: (String, Boolean)) = (ModulePrefix + path._1, path._2)

  private def asString(isApplicable: Boolean) = if (isApplicable) "applicable" else "not applicable"
}

object ResourcesTest {
  private val Applicable = true
  private val NotApplicable = false
  private val ModulePrefix = "/some/module"
}
