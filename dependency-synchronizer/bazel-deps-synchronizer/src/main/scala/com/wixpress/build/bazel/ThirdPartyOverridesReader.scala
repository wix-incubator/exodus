package com.wixpress.build.bazel

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

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
