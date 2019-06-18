package com.wixpress.build.bazel

import better.files.File
import com.wixpress.build.maven.Coordinates

import scala.collection.mutable

trait TestOnlyTargetsResolver {

  def isTestOnlyTarget(artifactCoordinates: Coordinates): Boolean
}

class SocialModeTestOnlyTargetsResolver(maybeManagedDepsRepoPath: Option[String]) extends TestOnlyTargetsResolver {

  import SocialModeTestOnlyTargetsResolver._

  private val testOnlyTargets = mutable.Set.empty[String]

  loadFromFile()

  private def loadFromFile() = {
    maybeManagedDepsRepoPath
      .filterNot(_.isEmpty)
      .foreach { managedDepsRepoPath =>
        for (line <- (File(managedDepsRepoPath) / SocialModeTestOnlyTargetsFilePath).lines) {
          testOnlyTargets.add(line.trim)
        }
      }
  }

  def isTestOnlyTarget(artifactCoordinates: Coordinates) =
    testOnlyTargets.contains(artifactCoordinates.serialized)
}

object SocialModeTestOnlyTargetsResolver {
  val SocialModeTestOnlyTargetsFilePath = "social_mode/testonly_targets"
}
