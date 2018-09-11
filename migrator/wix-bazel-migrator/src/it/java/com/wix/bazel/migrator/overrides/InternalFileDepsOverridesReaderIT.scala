package com.wix.bazel.migrator.overrides

import com.fasterxml.jackson.core.JsonProcessingException
import com.wix.bazel.migrator.overrides.{InternalFileDepsOverrides, InternalFileDepsOverridesReader}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class InternalFileDepsOverridesReaderIT extends SpecificationWithJUnit {
  "InternalFileDepsOverridesReader" should {

    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("{invalid")

      InternalFileDepsOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "read overrides from generated json" in new Context {
      val originalOverrides = InternalFileDepsOverrides(someRuntimeOverrides, someCompileTimeOverrides)
      writeOverrides(objectMapper.writeValueAsString(originalOverrides))

      InternalFileDepsOverridesReader.from(repoRoot) mustEqual originalOverrides
    }

    "read overrides from manual json" in new Context {
      writeOverrides("""{
          |  "runtimeOverrides" : {
          |   "some/path/to/module/one" : {
          |     "some/path/to/code.java" : ["path/to/runtime/dependency.scala", "other/runtime/dep.scala"],
          |     "other/path/to/code.java" : ["other/path/to/runtime/dependency.scala"]
          |   },
          |   "some/path/to/module/two" : {
          |     "last/path/to/code.scala" : ["last/path/to/runtime/dependency.scala"]
          |   }
          |  },
          |
          |  "compileTimeOverrides" : {
          |   "some/path/to/module/one" : {
          |     "some/path/to/code.java" : ["path/to/compile/dependency.scala", "other/compile/dep.scala"],
          |     "other/path/to/code.java" : ["other/path/to/compile/dependency.scala"]
          |   },
          |   "some/path/to/module/three" : {
          |     "last/path/to/code.scala" : ["last/path/to/compile/dependency.scala"]
          |   }
          |  }
          |}""".stripMargin)

      val overrides = InternalFileDepsOverridesReader.from(repoRoot)

      overrides.runtimeOverrides.get("some/path/to/module/one")("some/path/to/code.java") ====
              List("path/to/runtime/dependency.scala", "other/runtime/dep.scala")
      overrides.runtimeOverrides.get("some/path/to/module/one")("other/path/to/code.java") ====
              List("other/path/to/runtime/dependency.scala")
      overrides.runtimeOverrides.get("some/path/to/module/two")("last/path/to/code.scala") ====
              List("last/path/to/runtime/dependency.scala")

      overrides.compileTimeOverrides.get("some/path/to/module/one")("some/path/to/code.java") ====
              List("path/to/compile/dependency.scala", "other/compile/dep.scala")
      overrides.compileTimeOverrides.get("some/path/to/module/three")("last/path/to/code.scala") ====
              List("last/path/to/compile/dependency.scala")
    }

    "have sensible defaults when reading an empty json object to support specifying either compile/runtime on their own" in new Context {
      writeOverrides("{}")

      val partialOverrides = InternalFileDepsOverridesReader.from(repoRoot)

      partialOverrides.compileTimeOverrides must beNone
      partialOverrides.runtimeOverrides must beNone
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {

      val partialOverrides = InternalFileDepsOverridesReader.from(repoRoot)

      partialOverrides.compileTimeOverrides must beNone
      partialOverrides.runtimeOverrides must beNone
    }

  }

  abstract class Context extends Scope with OverridesReaderITSupport {

    override val overridesPath = setupOverridesPath(repoRoot, "internal_file_deps.overrides")

    def someRuntimeOverrides = someModuleOverrides("runtime")

    def someCompileTimeOverrides = someModuleOverrides("compile")

    private def someModuleOverrides(classifier: String): Option[Map[String, Map[String, List[String]]]] = Some(
      { 1 to 10 }
        .map {
          index =>
            s"module$index" -> someFileOverrides(classifier)
        }.toMap
    )

    private def someFileOverrides(classifier: String): Map[String, List[String]] = {
      { 1 to 10 }
        .map {
          index =>
            s"/path$index/file$index.java" ->
              List(
                s"path/to/dependency/$classifier$index.scala",
                s"path/to/other/dependency/$classifier$index"
              )
        }
        .toMap
    }
  }

}
