package com.wix.bazel.migrator.model

import com.wix.bazel.migrator.model.Language.Unknown

sealed trait Language {
  protected def +(language: Language): Language = languagesAddition.getOrElse(language, Unknown)
  protected def languagesAddition: Map[Language, Language]
}

object Language {

  case object Java extends Language {
    override protected def languagesAddition = Map(Java -> Java, Scala -> JavaScala)
  }

  case object Scala extends Language {
    override protected def languagesAddition = Map(Scala -> Scala, Java -> JavaScala)
  }

  case object JavaScala extends Language {
    override protected def languagesAddition = Map(Scala -> JavaScala, Java -> JavaScala)
  }

  case object Unknown extends Language {
    override protected def languagesAddition = Map()
  }

  private case object Empty extends Language {
    override protected def languagesAddition = Map(Scala -> Scala, Java -> Java)
  }

  def reduce(languages: Iterable[Language]): Language =
    languages.fold(Empty)(_ + _)

  //todo do we want to handle lower casing here or in the resource key. I think in resource key
  def from(extension: String): Language = extension match {
    case "java" => Language.Java
    case "scala" => Language.Scala
    case _ => Language.Unknown
  }
  
  implicit class ReduceLanguagesToLanguage(languages: Iterable[Language]) {
    def reduceToLanguage: Language = Language.reduce(languages)
  }

}

