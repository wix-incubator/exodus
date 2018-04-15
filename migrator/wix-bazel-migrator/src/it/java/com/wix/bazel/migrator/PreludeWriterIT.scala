package com.wix.bazel.migrator


class PreludeWriterIT extends BaseWriterIT {
  "PreludeWriter" should {
    "write empty BUILD file (since Bazel requires the dir to be a bazel package)" in new ctx {
      writer.write()

      path(withName = "BUILD.bazel") must beEmptyRegularFile
    }

    "write prelude_bazel file with default content" in new ctx {
      writer.write()

      path(withName = "prelude_bazel") must beRegularFile(withContent = Seq(PreludeWriter.ScalaLibraryImport, PreludeWriter.ScalaImport))
    }

    //API for tests
    "write prelude_bazel file with given content" in new ctx {
      val randomContent = Seq("some", random, "content")
      override val writer = new PreludeWriter(repoRoot, preludeContent = randomContent)

      writer.write()

      path(withName = "prelude_bazel") must beRegularFile(withContent = randomContent)
    }
  }

  abstract class ctx extends baseCtx {
    val writer = new PreludeWriter(repoRoot)

    override def path(withName: String) = repoRoot.resolve(s"tools/build_rules/$withName")
  }
}
