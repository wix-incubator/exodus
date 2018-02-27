package com.wix.bazel.migrator.workspace

import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification

trait WorkspaceMatchers { this: Specification =>
  def failWith(log: Matcher[String]): Matcher[WorkspaceBuildResult] = {
    not(equalTo(0)) ^^ { (_: WorkspaceBuildResult).code aka "Workspace build return code" } and
      log ^^ { (_: WorkspaceBuildResult).log aka "Workspace build log" }
  }
}
