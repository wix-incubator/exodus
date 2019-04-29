package com.wix.bazel.migrator.transform

import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.concurrent.atomic.AtomicInteger

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model._
import com.wixpress.build.maven.MavenScope
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.parallel.ParMap

//this is needed since currently the transformer isn't thread safe but the dependency analyzer is
class CachingEagerEvaluatingCodotaDependencyAnalyzer(sourceModules: Set[SourceModule], dependencyAnalyzer: DependencyAnalyzer) extends DependencyAnalyzer {
  private val log = LoggerFactory.getLogger(getClass)
  private val cachePath = Files.createDirectories(Paths.get("./cache"))
  private val objectMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .registerModule(new RelativePathSupportingModule)
    .registerModule(new SourceModuleSupportingModule(sourceModules))
    .addMixIn(classOf[Target], classOf[TypeAddingMixin])
    .addMixIn(classOf[CodePurpose], classOf[TypeAddingMixin])
    .addMixIn(classOf[TestType], classOf[TypeAddingMixin])
    .addMixIn(classOf[MavenScope], classOf[TypeAddingMixin])

  private val collectionType = objectMapper.getTypeFactory.constructCollectionType(classOf[util.Collection[Code]], classOf[Code])
  private val clean = sys.props.getOrElse("clean.dependency.analysis.cache", "") == "true"

  private def cachePathForSourceModule(m: SourceModule) = {
    cachePath.resolve(m.relativePathFromMonoRepoRoot + ".cache")
  }

  private val size = sourceModules.size
  private val counter = new AtomicInteger()
  private val tenthSize = size / 10

  private def initCachePathForSourceModule(p: Path) = Files.createDirectories(p.getParent)

  private def maybeCodeFromCache(p: Path): Option[List[Code]] = {
    if (clean || !Files.exists(p)) return None
    try {
      val value: util.Collection[Code] = objectMapper.readValue(p.toFile, collectionType)
      val codeList = value.asScala.toList
      Some(codeList)
    } catch {
      case e: Exception =>
        log.warn(s"Error reading $p ,deleting cache file.")
        log.warn(e.getMessage)
        Files.deleteIfExists(p)
        None
    }
  }

  private def retrieveCodeAndCache(m: SourceModule, cachePath: Path): List[Code] = {
    val codeList = dependencyAnalyzer.allCodeForModule(m)
    Files.deleteIfExists(cachePath)
    initCachePathForSourceModule(cachePath)
    Files.createFile(cachePath)
    try {
      objectMapper.writeValue(cachePath.toFile, codeList)
    } catch {
      case e: InterruptedException =>
        log.warn(s"aborting write to file $cachePath")
        Files.deleteIfExists(cachePath)
        throw e
      case e: Exception =>
        log.warn(s"could not write to file $cachePath")
        log.warn(e.getMessage)
    }
    codeList
  }

  private def calculateMapEntryFor(sourceModule: SourceModule) = {
    printProgress()
    val cachePath = cachePathForSourceModule(sourceModule)
    (sourceModule, maybeCodeFromCache(cachePath).getOrElse(retrieveCodeAndCache(sourceModule, cachePath)))
  }

  private def printProgress(): Unit = {
    if (tenthSize > 0) {
      val currentCount = counter.incrementAndGet()
      if (currentCount % tenthSize == 0) {
        log.info(s"DependencyAnalyzer:allCodeForModule:\t ${currentCount / tenthSize * 10}% done")
      }
    }
  }

  private val allCode: ParMap[SourceModule, List[Code]] = sourceModules.par.map(calculateMapEntryFor).toMap

  override def allCodeForModule(module: SourceModule): List[Code] = allCode(module)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "__class")
trait TypeAddingMixin