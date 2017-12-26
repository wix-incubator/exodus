package com.wix.bazel.migrator.model;
//TODO consider moving to sealed trait and case objects. should do it when chosen serialization will support case object
//contains unused types since they are used by the other glue project which is being merged in slowly
public enum Scope {
    PROVIDED, TEST_COMPILE, PROD_COMPILE, PROD_RUNTIME, TEST_RUNTIME
}