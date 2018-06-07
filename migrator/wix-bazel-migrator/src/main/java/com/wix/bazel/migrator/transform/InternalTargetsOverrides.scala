package com.wix.bazel.migrator.transform

case class InternalTargetsOverrides(targetOverrides: Set[InternalTargetOverride] = Set.empty)

case class InternalTargetOverride(label: String,
                                  testOnly: Option[Boolean] = None,
                                  testType: Option[String] = None, //testtype
                                  testSize: Option[String] = None,
                                  tags: Option[String] = None, //testtype
                                  additionalJvmFlags: Option[List[String]] = None,
                                  additionalDataDeps: Option[List[String]] = None,
                                  dockerImagesDeps: Option[List[String]] = None,
                                  newName: Option[String] = None,
                                  additionalProtoAttributes : Option[String] = None,
                                  blockNetwork: Option[Boolean] = None
                                 )

