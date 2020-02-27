package com.wix.bazel.migrator.analyze

import java.io.IOException
import java.nio.file.{Path, Paths}

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule

class RelativePathSupportingModule extends SimpleModule {
  addDeserializer(classOf[Path], new RelativePathSupportingDeserializer)
  addSerializer(classOf[Path], new RelativePathSupportingSerializer)
}

class RelativePathSupportingSerializer extends JsonSerializer[Path] {
  @throws[IOException]
  def serialize(value: Path, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    value match {
      case null => gen.writeNull()
      case _ => gen.writeString(value.toString)
    }
}

class RelativePathSupportingDeserializer extends JsonDeserializer[Path] {
  @throws[IOException]
  def deserialize(p: JsonParser, ctxt: DeserializationContext): Path =
    p.getCurrentToken match {
      case JsonToken.VALUE_NULL => null
      case JsonToken.VALUE_STRING => Paths.get(p.readValueAs(classOf[String]))
      case _ => throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, "The value of a java.nio.file.Path must be a string")
    }
}