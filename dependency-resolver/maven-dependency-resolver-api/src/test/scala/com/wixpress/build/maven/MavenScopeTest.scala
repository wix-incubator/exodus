package com.wixpress.build.maven

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.core.{Fragment, Fragments}

class MavenScopeTest extends SpecificationWithJUnit {
  val ScopesToNames = List(
    ScopeToName(MavenScope.Compile,"compile"),
    ScopeToName(MavenScope.Test,"test"),
    ScopeToName(MavenScope.Runtime,"runtime"),
    ScopeToName(MavenScope.Provided,"provided"),
    ScopeToName(MavenScope.System,"system")
  )
  private def extractTest(scopeToName:ScopeToName):Fragment ={
    s"parse ${scopeToName.scope} from string '${scopeToName.name}'" in {
      MavenScope.of(scopeToName.name) mustEqual scopeToName.scope
    }
  }

  def allTests:Fragments = Fragments(ScopesToNames.map(extractTest): _*)

  "MavenScope" should {
    allTests
  }

}

case class ScopeToName(scope:MavenScope, name:String)
