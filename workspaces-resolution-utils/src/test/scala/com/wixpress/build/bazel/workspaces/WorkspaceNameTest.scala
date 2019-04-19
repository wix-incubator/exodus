package com.wixpress.build.bazel.workspaces

import org.specs2.mutable.SpecificationWithJUnit

class WorkspaceNameTest extends SpecificationWithJUnit {
  "WorkspaceName" should {
    "return short workspace name according to organization and repo name of given git url" in {
      val repoName = "name"
      val repoOrganization = "orgnization"
      val gitUrl = "git@github.com:" + repoOrganization + "/" + repoName + ".git"

      WorkspaceName.by(gitUrl) mustEqual s"orgnization_name"
    }

    "throw exception in case of malformed URL is given" in {
      WorkspaceName.by("some.bad.url") must throwA[InvalidGitURLException]
    }

    "normalize elements with dash to underscore" in {
      val repoName = "some-name"
      val org = "some-org"
      val gitUrl = "git@github.com:" + org + "/" + repoName + ".git"

      WorkspaceName.by(gitUrl) mustEqual s"some_org${WorkspaceName.Separator}some_name"
    }

    "normalize elements with dots to underscore" in {
      val repoName = "some.name"
      val org = "some.org"
      val gitUrl = "git@github.com:" + org + "/" + repoName + ".git"

      WorkspaceName.by(gitUrl) mustEqual s"some_org${WorkspaceName.Separator}some_name"
    }

    "drop organization part for repositories in default org (wix-private)" in {
      val repoName = "name"
      val org = "wix-private"
      val gitUrl = "git@github.com:" + org + "/" + repoName + ".git"

      WorkspaceName.by(gitUrl) mustEqual "name"
    }

    "extract organization according to organization of given git url" in {
      val repoName = "name"
      val repoOrganization = "some_orgnization"
      val gitUrl = "git@github.com:" + repoOrganization + "/" + repoName + ".git"

      WorkspaceName.extractOrganization(gitUrl) mustEqual repoOrganization
    }

    "throw exception in case of malformed url is given when extracting organization" in {
      WorkspaceName.extractOrganization("some.bad.url") must throwA[InvalidGitURLException]
    }
  }
}
