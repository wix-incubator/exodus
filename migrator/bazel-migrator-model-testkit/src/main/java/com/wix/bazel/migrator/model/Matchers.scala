package com.wix.bazel.migrator.model

import com.wix.bazel.migrator.model.Target.TargetDependency
import org.specs2.matcher.Matchers._
import org.specs2.matcher.{AlwaysMatcher, MatchFailure, Matcher, MustExpectable}

import scala.reflect.{ClassTag, classTag}

object Matchers {
  def a[A, T: ClassTag](matcher: Matcher[T]): Matcher[A] =
    beLike[A] {
      case value: T => matcher(MustExpectable[T](value))
      case value => MatchFailure("", s"is not of type ${classTag[T].runtimeClass.getSimpleName}", MustExpectable(value))
    }

  def protoTarget(name: String,
                  belongsToPackage: Matcher[String] = AlwaysMatcher[String](),
                  dependencies: Matcher[Set[Target]] = AlwaysMatcher[Set[Target]]()
                 ): Matcher[Target.Proto] =
    aTarget(name, belongsToPackage) and
      dependencies ^^ {
        (_: Target.Proto).dependencies aka "dependencies"
      }

  def aPackageWithMultipleTargets(relativePath: Matcher[String] = AlwaysMatcher[String](),
                                  targets: Matcher[Set[Target]],
                                  originatingSourceModule: Matcher[SourceModule] = AlwaysMatcher[SourceModule]()): Matcher[Package] = {
    relativePath ^^ {
      (_: Package).relativePathFromMonoRepoRoot aka "relative path from mono repo root"
    } and targets ^^ {
      (_: Package).targets aka "targets"
    } and originatingSourceModule ^^ {
      (_: Package).originatingSourceModule aka "originating source module"
    }
  }

  def aPackage(relativePath: Matcher[String] = AlwaysMatcher[String](),
               target: Matcher[Target] = AlwaysMatcher[Target](),
               originatingSourceModule: Matcher[SourceModule] = AlwaysMatcher[SourceModule]()): Matcher[Package] =
    aPackageWithMultipleTargets(relativePath, contain(target), originatingSourceModule)


  def aTarget(name: String,
              belongsToPackage: Matcher[String] = AlwaysMatcher[String]()
             ): Matcher[Target] =
    be_===(name) ^^ {
      (_: Target).name aka "target name"
    } and belongsToPackage ^^ {
      (_: Target).belongingPackageRelativePath aka "belonging package relative path"
    }

  def jvmTarget(name: String,
                sources: Matcher[Set[String]] = AlwaysMatcher[Set[String]](),
                dependencies: Matcher[Set[TargetDependency]] = AlwaysMatcher[Set[TargetDependency]](),
                codePurpose: Matcher[CodePurpose] = AlwaysMatcher[CodePurpose](),
                originatingSourceModule: Matcher[SourceModule] = AlwaysMatcher[SourceModule]()
               ): Matcher[Target.Jvm] = {
    be_===(name) ^^ {
      (_: Target.Jvm).name aka "target name"
    } and
      sources ^^ {
        (_: Target.Jvm).sources aka "sources"
      } and
      dependencies ^^ {
        (_: Target.Jvm).dependencies aka "dependencies"
      } and
      codePurpose ^^ {
        (_: Target.Jvm).codePurpose aka "code purpose"
      } and
      originatingSourceModule ^^ {
        (_: Target.Jvm).originatingSourceModule aka "originating source module"
      }
  }

}
