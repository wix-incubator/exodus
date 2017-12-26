package com.wix.bazel.migrator.model

import com.wix.bazel.migrator.model.Language.{Java, Scala, Unknown}
import org.specs2.mutable.SpecificationWithJUnit

class LanguageTest extends SpecificationWithJUnit {
  "Language.from" should {

    "return Unknown for unknown strings" in {
      Language.from("something") ==== Language.Unknown
    }

  }

  "reduceToLanguage should return Unknown for" >> {

    "Java and Unknown extensions" in {
      Seq(Java, Unknown).reduceToLanguage ==== Language.Unknown
    }

    "Unknown and Java extensions" in {
      Seq(Unknown, Java).reduceToLanguage ==== Language.Unknown
    }

    "Scala and Unknown extensions" in {
      Seq(Unknown, Scala).reduceToLanguage ==== Language.Unknown
    }

    "Unknown and Scala extensions" in {
      Seq(Scala, Unknown).reduceToLanguage ==== Language.Unknown
    }
  }

}
