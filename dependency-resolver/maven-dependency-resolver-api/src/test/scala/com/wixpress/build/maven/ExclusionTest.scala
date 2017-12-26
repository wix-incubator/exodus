package com.wixpress.build.maven

import org.specs2.mutable.SpecificationWithJUnit
import org.eclipse.aether.graph.{Exclusion => AetherExclusion}

class ExclusionTest extends SpecificationWithJUnit {
  private val someExclusion = Exclusion("some.group","some-artifact")
  private val AnyWildCard = "*"
  "Exclusion" should {

    "convert from Aether Exclusion" in {
      val aetherExclusion = new AetherExclusion(someExclusion.groupId,someExclusion.artifactId,AnyWildCard,AnyWildCard)

      Exclusion.fromAetherExclusion(aetherExclusion) mustEqual someExclusion
    }

    "convert to Aether Exclusion" in {
      val aetherExclusion = someExclusion.toAetherExclusion

      aetherExclusion.getGroupId mustEqual someExclusion.groupId
      aetherExclusion.getArtifactId mustEqual someExclusion.artifactId
      aetherExclusion.getExtension mustEqual AnyWildCard
      aetherExclusion.getClassifier mustEqual AnyWildCard
    }
  }
}
