package com.wix.build.sync.api


import com.wixpress.build.maven.Coordinates


object BazelSyncGreyhoundEvents{
  val BazelManagedDepsSyncEndedTopic : String = "BazelManagedDepsSyncEndedTopic"
}

case class ThirdPartyArtifact(groupId : String,
                              artifactId : String,
                              version : String,
                              packaging : String,
                              classifier: Option[String] = None,
                              digest : Option[String]){
  def label() : String = {
    val gid = groupId.replaceAll("-","_").replaceAll("\\.","_")
    val aid = artifactId.replaceAll("-","_").replaceAll("\\.","_")
    s"@${gid}_${aid}"
  }
}

case class BazelManagedDepsSyncEnded(thirdPartyArtifacts : Set[ThirdPartyArtifact])