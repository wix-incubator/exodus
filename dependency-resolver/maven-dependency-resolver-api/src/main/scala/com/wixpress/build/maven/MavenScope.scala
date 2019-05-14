package com.wixpress.build.maven

abstract class MavenScope(val name: String) {

  //required since jackson creates a new instance when deserializing:
  // https://github.com/FasterXML/jackson-module-scala/issues/211
  override def equals(obj: Any): Boolean = {
    obj.isInstanceOf[MavenScope] && obj.asInstanceOf[MavenScope].name.equals(name)
  }
}

object MavenScope {

  case object Runtime extends MavenScope("runtime")

  case object Compile extends MavenScope("compile")

  case object Test extends MavenScope("test")

  case object Provided extends MavenScope("provided")

  case object System extends MavenScope("system")

  private val allScopes = List(Runtime, Compile, Test, Provided, System)

  def of(scopeName: String): MavenScope = allScopes.find(_.name.equalsIgnoreCase(scopeName))
    .getOrElse(MavenScope.Compile)
}