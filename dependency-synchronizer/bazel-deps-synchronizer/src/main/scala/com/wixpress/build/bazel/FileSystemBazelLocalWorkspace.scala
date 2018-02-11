package com.wixpress.build.bazel

import java.io.FileNotFoundException

import better.files.File
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class FileSystemBazelLocalWorkspace(root: File) extends BazelLocalWorkspace {

  private val ThirdPartyOverridesPath = "bazel_migration/third_party_targets.overrides"

  validate()

  override def overwriteBuildFile(packageName: String, content: String): Unit = {
    val buildFilePath = root / packageName / "BUILD.bazel"
    buildFilePath.createIfNotExists(createParents = true)
    buildFilePath.overwrite(content)
  }

  override def overwriteThirdPartyReposFile(thirdPartyReposContent: String): Unit =
    (root / thirdPartyReposFilePath).overwrite(thirdPartyReposContent)

  override def thirdPartyReposFileContent(): String = contentIfExistsOf(root / thirdPartyReposFilePath).getOrElse("")

  override def buildFileContent(packageName: String): Option[String] = contentIfExistsOf(root / packageName / "BUILD.bazel")

  override def thirdPartyOverrides(): ThirdPartyOverrides = {
    contentIfExistsOf(root / ThirdPartyOverridesPath)
      .map(ThirdPartyOverridesReader.from)
      .getOrElse(ThirdPartyOverrides.empty)
  }

  private def contentIfExistsOf(filePath: File) =
    if (filePath.exists) Some(filePath.contentAsString) else None


  private def validate(): Unit = {
    if (!root.exists)
      throw new FileNotFoundException(root.pathAsString)
  }

}

object ThirdPartyOverridesReader {

  def from(json: String): ThirdPartyOverrides = {
    mapper.readValue(json, classOf[ThirdPartyOverrides])
  }

  def mapper: ObjectMapper = {
    val objectMapper = new ObjectMapper()
      .registerModule(DefaultScalaModule)
    objectMapper.registerModule(overrideCoordinatesKeyModule(objectMapper))
    objectMapper
  }

  private def overrideCoordinatesKeyModule(mapper: ObjectMapper): Module =
    new SimpleModule()
      .addKeyDeserializer(classOf[OverrideCoordinates], new OverrideCoordinatesKeyDeserializer(mapper))
      .addKeySerializer(classOf[OverrideCoordinates], new OverrideCoordinatesKeySerializer(mapper))

  private class OverrideCoordinatesKeySerializer(mapper: ObjectMapper) extends JsonSerializer[OverrideCoordinates] {
    override def serialize(value: OverrideCoordinates, gen: JsonGenerator, serializers: SerializerProvider): Unit =
      gen.writeFieldName(value.groupId + ":" + value.artifactId)
  }

  private class OverrideCoordinatesKeyDeserializer(mapper: ObjectMapper) extends KeyDeserializer {
    override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef =
      key.split(':') match {
        case Array(groupId, artifactId) => OverrideCoordinates(groupId, artifactId)
        case _ => throw new IllegalArgumentException(s"OverrideCoordinates key should be in form of groupId:artifactId, got $key")
      }
  }

}