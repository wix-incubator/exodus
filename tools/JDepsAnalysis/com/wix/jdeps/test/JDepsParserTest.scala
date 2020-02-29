package com.wix.jdeps.test

import java.nio.file.Files

import com.wix.bazel.migrator.model.{ModuleDependencies, SourceModule}
import com.wix.jdeps.{ClassDependencies, JDepsParserImpl, JVMClass}
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit




class JDepsParserTest extends SpecificationWithJUnit {
  "JDepsParser" should {
    "remove 3rd party deps" in {
      val jdepsOutput = Files.createTempFile("jdeps-output", ".txt")

      Files.write(jdepsOutput, jdepsOutputContent.getBytes )

      val coreCommon =
        SourceModule("commons/core-common", Coordinates("exodus-demo.commons", "core-common", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies())
      val coreBusinessSomeService = SourceModule("products/some-service/core-business-some-service", Coordinates("exodus-demo.products", "core-business-some-service", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies())
      val coreRepositorySomeRepo = SourceModule("repositories/some-repository/core-repository-some-repo", Coordinates("exodus-demo.repositories", "core-repository-some-repo", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies())
      val parser = new JDepsParserImpl(Set(
              coreCommon,
              coreBusinessSomeService,
              coreRepositorySomeRepo,
            ))
      parser.convert(ClassDependencies(jdepsOutput), coreBusinessSomeService) mustEqual Map(
        JVMClass("exodus.demo.core.business.some.service.SomeServiceCoreConfiguration",coreBusinessSomeService) -> Set.empty,
        JVMClass("exodus.demo.core.business.some.service.api.SomeService",coreBusinessSomeService) -> Set.empty,
        JVMClass("exodus.demo.core.business.some.service.impl.DefaultSomeService", coreBusinessSomeService) -> Set(
          JVMClass("exodus.demo.commons.core.something.SomeCommonBusinessUtil", coreCommon),
          JVMClass("exodus.demo.core.business.some.service.api.SomeService", coreBusinessSomeService),
          JVMClass("exodus.demo.core.repositories.some.repo.SomeRepository", coreRepositorySomeRepo),
        )
      )
    }
  }

  val jdepsOutputContent: String = """digraph "core-business-some-service-0.0.1-SNAPSHOT.jar" {
                             |    // Path: products/some-service/core-business-some-service/target/core-business-some-service-0.0.1-SNAPSHOT.jar
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration"     -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" ->    "org.springframework.context.annotation.ComponentScan (not found)";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration"   ->    "org.springframework.context.annotation.Configuration (not found)";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" -> "org.springframework.context.annotation.Import (not found)";
                             |   "exodus.demo.core.business.some.service.api.SomeService" -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "exodus.demo.commons.core.something.SomeCommonBusinessUtil (core-common-0.0.1-SNAPSHOT.jar)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService"   -> "exodus.demo.core.business.some.service.api.SomeService (classes)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" ->    "exodus.demo.core.repositories.some.repo.SomeRepository (core-repository-some-repo-0.0.1-SNAPSHOT.jar)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.IllegalStateException";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.String";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "org.springframework.stereotype.Service (not found)";
                             |}
                             |
                             |""".stripMargin
}
