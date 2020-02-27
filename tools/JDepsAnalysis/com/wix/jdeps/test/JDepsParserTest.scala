package com.wix.jdeps.test

import java.nio.file.Files

import com.wix.bazel.migrator.model.{ModuleDependencies, SourceModule}
import com.wix.jdeps.{AnalysisModule, ClassDependencies, JDepsParserImpl, JVMClass}
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit




class JDepsParserTest extends SpecificationWithJUnit {
  "JDepsParser" should {
    "remove 3rd party deps" in {
      val jdepsOutput = Files.createTempFile("jdeps-output", ".txt")

      Files.write(jdepsOutput, jdepsOutputContent.getBytes )

      val coreCommon = AnalysisModule(
        SourceModule("commons/core-common", Coordinates("exodus-demo.commons", "core-common", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies()),
        "commons/core-common/target/core-common-0.0.1-SNAPSHOT.jar")
      val coreBusinessSomeService = AnalysisModule(
        SourceModule("products/some-service/core-business-some-service", Coordinates("exodus-demo.products", "core-business-some-service", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies()),
        "products/some-service/core-business-some-service/target/core-business-some-service-0.0.1-SNAPSHOT.jar")
      val coreRepositorySomeRepo = AnalysisModule(
        SourceModule("repositories/some-repository/core-repository-some-repo", Coordinates("exodus-demo.repositories", "core-repository-some-repo", "0.0.1-SNAPSHOT"), Set.empty, ModuleDependencies()),
        "repositories/some-repository/core-repository-some-repo/target/")
      val parser = new JDepsParserImpl(Set(
        coreCommon.sourceModule,
        coreBusinessSomeService.sourceModule,
        coreRepositorySomeRepo.sourceModule,
      ))
      parser.convert(ClassDependencies(jdepsOutput), coreBusinessSomeService) mustEqual Map(
        JVMClass("exodus.demo.core.business.some.service.impl.DefaultSomeService", coreBusinessSomeService.sourceModule) -> Set(
          JVMClass("exodus.demo.commons.core.something.SomeCommonBusinessUtil", coreCommon.sourceModule),
          JVMClass("exodus.demo.core.business.some.service.api.SomeService", coreBusinessSomeService.sourceModule),
          JVMClass("exodus.demo.core.repositories.some.repo.SomeRepository", coreRepositorySomeRepo.sourceModule),
        )
      )
    }
  }

  val jdepsOutputContent = """digraph "core-business-some-service-0.0.1-SNAPSHOT.jar" {
                             |    // Path: products/some-service/core-business-some-service/target/core-business-some-service-0.0.1-SNAPSHOT.jar
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" -> "org.springframework.context.annotation.ComponentScan (not found)";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" -> "org.springframework.context.annotation.Configuration (not found)";
                             |   "exodus.demo.core.business.some.service.SomeServiceCoreConfiguration" -> "org.springframework.context.annotation.Import (not found)";
                             |   "exodus.demo.core.business.some.service.api.SomeService" -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "exodus.demo.commons.core.something.SomeCommonBusinessUtil (core-common-0.0.1-SNAPSHOT.jar)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "exodus.demo.core.business.some.service.api.SomeService (classes)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "exodus.demo.core.repositories.some.repo.SomeRepository (core-repository-some-repo-0.0.1-SNAPSHOT.jar)";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.IllegalStateException";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.Object";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "java.lang.String";
                             |   "exodus.demo.core.business.some.service.impl.DefaultSomeService" -> "org.springframework.stereotype.Service (not found)";
                             |}
                             |
                             |""".stripMargin
}
