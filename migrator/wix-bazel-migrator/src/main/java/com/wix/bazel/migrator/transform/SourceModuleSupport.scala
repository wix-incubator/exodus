package com.wix.bazel.migrator.transform

import java.io.IOException

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.wix.bazel.migrator.model.SourceModule

class SourceModuleSupportingModule(modules: Set[SourceModule]) extends SimpleModule {
  addDeserializer(classOf[SourceModule], new SourceModuleSupportingDeserializer(modules))
  addSerializer(classOf[SourceModule], new SourceModuleSupportingSerializer)
}

class SourceModuleSupportingSerializer extends JsonSerializer[SourceModule] {
  @throws[IOException]
  def serialize(value: SourceModule, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    value match {
      case null => gen.writeNull()
      case _ => gen.writeString(value.relativePathFromMonoRepoRoot)
    }
}

class SourceModuleSupportingDeserializer(modules: Set[SourceModule]) extends JsonDeserializer[SourceModule] {
  @throws[IOException]
  def deserialize(p: JsonParser, ctxt: DeserializationContext): SourceModule =
    p.getCurrentToken match {
      case JsonToken.VALUE_NULL => null
      case JsonToken.VALUE_STRING => {
        val relativePath = p.readValueAs(classOf[String])
        modules.find(_.relativePathFromMonoRepoRoot == relativePath)
          .getOrElse(throw ctxt.weirdStringException(relativePath, classOf[SourceModule], s"could not find module with relative path for $relativePath"))
      }
      case token => throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, s"The value of a module must be a string and currently is $token")
    }
}