package com.wix.bazel.migrator.transform

import java.nio.file.Paths

import com.wix.bazel.migrator.SourceModules
import com.wix.bazel.migrator.model.SourceModule

class ManualInfoDependencyAnalyzer(sourceModules: SourceModules) extends DependencyAnalyzer {
  private val WixFrameworkModule = moduleForRelativePath("wix-framework")
  private val cflogJsonLayoutDep = dependencyOn("hoopoe-cflog-framework/hoopoe-cflog/src/main/java/com/wixpress/framework/logging/CFLogJSONLayout.java")
  private val markerFilterDep = dependencyOn("logging-modules/wix-logback-extensions/src/main/java/com/wixpress/framework/logging/logBackFilters/MarkerFilter.java")
  private val testConsoleAppenderDep = dependencyOn("hoopoe-common/hoopoe-utest/src/main/java/com/wixpress/hoopoe/test/TestConsoleAppender.java")

  private val info: Map[SourceModule, List[Code]] = Map(
    /*
    The following list is not the complete list of dependencies these xml files have.
    I've de-duped files which fall under the same bazel package to minimize the size of the graph.
    If these files were to move the correctness might be broken.
     */
    /*
    Problem with the following definition is that it causes a cycle
    (every source code, like the one ErrorHandlingAppITConfig belongs to, depends by itself on src/it/resources)
        ErrorHandlingTestAppModule -> List(
            Code(
              CodePath(ErrorHandlingTestAppModule, "src/it/resources", Paths.get("embedded-servers-beans.xml")),
              List(Dependency(
                CodePath(
                  ErrorHandlingTestAppModule,
                  "src/it/java",
                  Paths.get("com/wixpress/framework/exceptions/app/it/ErrorHandlingAppITConfig.scala")))
              )
            )
          )
    */
    WixFrameworkModule -> List(
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-logging-config.xml"),
        List(
          dependencyOn("metrics-modules/wix-metrics-core/src/main/scala/com/wixpress/framework/logging/AnnotationBasedLogMetadataBuilder.java"),
          dependencyOn("metrics-modules/wix-metrics-core/src/main/scala/com/wixpress/framework/monitoring/metering/MetricsConfig.java"),
          dependencyOn("hoopoe-cflog-framework/hoopoe-cflog/src/main/java/com/wixpress/framework/logging/CFLogJSONMessageFormatter.java"),
          dependencyOn("hoopoe-common/hoopoe-reflection/src/main/java/com/wixpress/hoopoe/reflection/parameters/PrioritizedParameterNamesDiscoverer.java"),
          dependencyOn("logging-modules/wix-logback-extensions/src/main/java/com/wixpress/framework/logging/retriever/LogRetrieverManager.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-json-oxm-config.xml"),
        List(
          dependencyOn("hoopoe-json/src/main/java/com/wixpress/framework/oxm/JacksonObjectMapperFactoryBean.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-i18n-config.xml"),
        List(
          dependencyOn("localization-modules/wix-localization/src/main/java/com/wixpress/framework/i18n/I18NSpringConfig.scala")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-health-config.xml"),
        List(
          dependencyOn("management-modules/hoopoe-management-framework/hoopoe-management-spring/src/main/java/com/wixpress/framework/health/HealthMonitoringConfig.scala")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-base-config.xml"),
        List(
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/domain/StringToGuidConverter.java"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/context/JsCallbackContextFactory.java"),
          dependencyOn("app-info-modules/wix-app-info-spring/src/main/java/com/wixpress/framework/appInfo/ServletContextAppInfoBeanFactory.scala"),
          dependencyOn("request-lifecycle-modules/wix-framework-web-context/src/main/scala/com/wixpress/framework/context/WebContextFactory.java"),
          dependencyOn("request-lifecycle-modules/wix-framework-mvc-request-aspect/src/main/scala/com/wixpress/framework/context/MvcRequestAspectFactory.scala"),
          dependencyOn("request-lifecycle-modules/wix-framework-response-context/src/main/scala/com/wixpress/framework/context/ResponseContextFactory.java"),
          dependencyOn("request-lifecycle-modules/wix-framework-aspects-spring/src/main/scala/com/wixpress/framework/web/executionlifecycle/ExecutionLifecycleManager.java"),
          dependencyOn("error-handling-modules/wix-error-handling-spring/src/main/scala/com/wixpress/framework/exceptions/ExceptionMetadataHolder.scala"),
          dependencyOn("request-lifecycle-modules/wix-framework-aspects-spring/src/main/scala/com/wixpress/framework/aspects/AspectsSpringConfig.scala"),
          dependencyOn("request-lifecycle-modules/wix-framework-request-aspect-api/src/main/scala/com/wixpress/framework/context/ThreadLocalRequestAspectStore.scala"),
          dependencyOn("localization-modules/wix-localization/src/main/java/com/wixpress/framework/i18n/DefaultLocaleEnforcer.scala"),
          dependencyOn("hoopoe-common/hoopoe-time-utils/src/main/java/com/wixpress/framework/time/SystemTimeSource.java"),
          dependencyOn("hoopoe-spring/src/main/java/com/wixpress/framework/spring/WixNamespaceHandler.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-configuration-config.xml"),
        List(
          dependencyOn("hoopoe-spring/src/main/java/com/wixpress/framework/spring/PerWebappNamingStrategy.java"),
          dependencyOn("configuration-modules/hoopoe-configuration/src/main/java/com/wixpress/framework/configuration/ConfigurationManager.java"),
          dependencyOn("configuration-modules/hoopoe-configuration/src/main/java/com/wixpress/framework/configuration/metadata/DefaultConfigurationMetadataManager.java"),
          dependencyOn("configuration-modules/hoopoe-configuration-spring/src/main/java/com/wixpress/framework/configuration/spring/SpringConfigurationManagerAdapter.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-feature-toggle-config.xml"),
        List(
          dependencyOn("feature-toggle-modules/wix-feature-toggle-core/src/main/java/com/wixpress/framework/featuretoggle/FeatureToggleSpringConfig.scala")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-monitoring-config.xml"),
        List(
          dependencyOn("monitoring-modules/wix-newrelic-reporting/src/main/java/com/wixpress/framework/spring/NewRelicReportingConfig.scala")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-oxm-config.xml"),
        List(
          dependencyOn("web-utils-modules/wix-spring-xml-converter/src/main/java/com/wixpress/framework/oxm/WixJaxb2RootElementOxMapper.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-security-config.xml"),
        List(
          dependencyOn("security-modules/wix-framework-security/src/main/scala/com/wixpress/framework/security/SecuritySpringConfig.scala")
        )
      ),
      /*
      I think we can ignore this config. This isn't really generic but in wix-framework by mistake. I'd rather not have everyone get static grid facade.
              Code(
                codePathFrom("wix-framework/src/main/resources/wix-framework-static-grid-config.xml")
                List(
                  dependencyOn("static-grid-facade/src/main/java/com/wixpress/framework/staticgrid/StaticGridConfiguration.java"),
                  dependencyOn("static-grid-facade/src/main/java/com/wixpress/framework/staticgrid/spring/StaticGridFacadeBeansConfig.java")
                )
              ),
      */
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-validation-config.xml"),
        List(
          dependencyOn("validation-modules/hoopoe-validation/src/main/java/com/wixpress/framework/validation/WixApacheFactoryContext.java"),
          dependencyOn("validation-modules/wix-validation/src/main/java/com/wixpress/framework/validation/ValidationExceptionPayloadFactory.java")
        )
      ),
      Code(
        codePathFrom("wix-framework/src/main/resources/wix-framework-mvc-config-without-app-info-endpoints-meant-for-libraries-only.xml"),
        List(
          dependencyOn("metrics-modules/wix-metrics-core/src/main/scala/com/wixpress/framework/monitoring/metering/MeteringConfig.java"),
          dependencyOn("petri-integration-modules/petri-integration-annotations/src/main/scala/com/wixpress/framework/petri/DefaultsExperimentSpecMaterializer.scala"),
          dependencyOn("wix-http-buffer-controller/src/main/java/com/wixpress/framework/http/buffer/HttpBufferLimitationSpringConfig.java"),
          dependencyOn("web-utils-modules/wix-spring-xml-converter/src/main/java/com/wixpress/framework/oxm/WixJaxb2RootElementHttpMessageConverter.java"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/web/messageConverters/MappingJackson2HttpMessageConverter.java"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/web/converter/CustomConvertersFactoryBean.scala"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/web/mvc/ControllerMethodSavingInterceptor.scala"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/web/httpheaders/HttpHeadersEventHandler.java"),
          dependencyOn("error-handling-modules/wix-error-handling-spring/src/main/scala/com/wixpress/framework/exceptions/ExceptionResolvingSpringConfig.scala"),
          dependencyOn("error-handling-modules/wix-error-handling/src/main/java/com/wixpress/framework/exceptions/ExceptionLogger.java"),
          dependencyOn("monitoring-modules/wix-newrelic-reporting/src/main/java/com/wixpress/framework/spring/NewRelicReportingConfig.scala"),
          dependencyOn("hoopoe-spring/src/main/java/com/wixpress/framework/spring/ConverterRegisteringBeanPostProcessor.java"),
          dependencyOn("bi-modules/wix-bi-context/src/main/java/com/wixpress/framework/bi/BiBeansConfig.scala"),
          dependencyOn("validation-modules/wix-validation/src/main/java/com/wixpress/framework/validation/ValidationEventHandler.java"),
          dependencyOn("app-info-modules/wix-app-info-web-api/src/main/scala/com/wixpress/framework/appInfo/web/AppInfoService.scala"),
          dependencyOn("velocity-modules/hoopoe-velocity/src/main/java/com/wixpress/framework/web/velocity/TopologyTool.java"),
          dependencyOn("velocity-modules/hoopoe-velocity/src/main/java/com/wixpress/framework/topology/TopologyConfiguration.java"),
          dependencyOn("throttling-modules/wix-throttling/src/main/java/com/wixpress/framework/throttling/ThrottlerRegistry.java"),
          dependencyOn("bi-modules/wix-bi-reporting-aspects-event-sink/src/main/scala/com/wixpress/framework/bi/BIReportingAspectsEventsSpringConfig.scala")
        )
      )
    ),
    moduleForRelativePath("bi-modules/wix-bi-reporting") -> List(
      Code(
        codePathFrom("bi-modules/wix-bi-reporting/src/test/resources/logback-test.xml"),
        List(markerFilterDep, cflogJsonLayoutDep)
      )
    ),
    moduleForRelativePath("wix-framework-crossbreed-tests") -> List(
      Code(
        codePathFrom("wix-framework-crossbreed-tests/src/it/resources/logback-test.xml"),
        List(markerFilterDep, cflogJsonLayoutDep, testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("async-modules/wix-async") -> List(
      Code(
        codePathFrom("async-modules/wix-async/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("bootstrap-modules/wix-server-bootstrap-contract-tests") -> List(
      Code(
        codePathFrom("bootstrap-modules/wix-server-bootstrap-contract-tests/src/it/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("feature-toggle-modules/wix-feature-toggle-administration") -> List(
      Code(
        codePathFrom("feature-toggle-modules/wix-feature-toggle-administration/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("feature-toggle-modules/wix-feature-toggle-core") -> List(
      Code(
        codePathFrom("feature-toggle-modules/wix-feature-toggle-core/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("hoopoe-cflog-framework/hoopoe-cflog") -> List(
      Code(
        codePathFrom("hoopoe-cflog-framework/hoopoe-cflog/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("configuration-modules/hoopoe-configuration") -> List(
      Code(
        codePathFrom("configuration-modules/hoopoe-configuration/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("configuration-modules/hoopoe-configuration-spring") -> List(
      Code(
        codePathFrom("configuration-modules/hoopoe-configuration-spring/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("hoopoe-core") -> List(
      Code(
        codePathFrom("hoopoe-core/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("hoopoe-scala") -> List(
      Code(
        codePathFrom("hoopoe-scala/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("jdbc-modules/hoopoe-jdbc") -> List(
      Code(
        codePathFrom("jdbc-modules/hoopoe-jdbc/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("logging-modules/wix-logback-extensions") -> List(
      Code(
        codePathFrom("logging-modules/wix-logback-extensions/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep, markerFilterDep)
      )
    ),
    moduleForRelativePath("metrics-modules/wix-metrics-tracer-test-app") -> List(
      Code(
        codePathFrom("metrics-modules/wix-metrics-tracer-test-app/src/main/resources/logback.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("rpc-modules/hoopoe-rpc-contract-tests") -> List(
      Code(
        codePathFrom("rpc-modules/hoopoe-rpc-contract-tests/src/it/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("security-modules/wix-security") -> List(
      Code(
        codePathFrom("security-modules/wix-security/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("web-utils-modules/wix-http-cookies-test-app") -> List(
      Code(
        codePathFrom("web-utils-modules/wix-http-cookies-test-app/src/it/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("wix-integration-test-framework") -> List(
      Code(
        codePathFrom("wix-integration-test-framework/src/test/resources/logback-test.xml"),
        List(testConsoleAppenderDep)
      )
    ),
    moduleForRelativePath("wix-compatibility-integration-modules/wix-compatibility-integration-test-app") -> List(
      Code(
        codePathFrom("wix-compatibility-integration-modules/wix-compatibility-integration-test-app/src/it/resources/logback-test.xml"),
        List(testConsoleAppenderDep, cflogJsonLayoutDep)
      )
    ),
    moduleForRelativePath("wix-framework-war-bootstraping") -> List(
      Code(
        codePathFrom("wix-framework-war-bootstraping/src/main/resources/wix-framework-mvc-config-without-petri.xml"),
        List(
          dependencyOn("wix-framework/src/main/resources/wix-framework-mvc-config-without-app-info-endpoints-meant-for-libraries-only.xml"),
          dependencyOn("app-info-modules/wix-app-info-web/src/main/scala/com/wixpress/framework/appInfo/web/AppInfoBuiltinConfig.scala"),
          dependencyOn("configuration-modules/wix-configuration-app-info/src/main/scala/com/wixpress/framework/configuration/appInfo/web/ConfigurationAppInfoConfig.scala"),
          dependencyOn("rpc-modules/wix-rpc/src/main/scala/com/wixpress/framework/rpc/appInfo/web/RpcAppInfoConfig.java"),
          dependencyOn("feature-toggle-modules/wix-feature-toggle-core/src/main/java/com/wixpress/framework/featuretoggle/appInfo/web/FeatureToggleAppInfoConfig.java"),
          dependencyOn("logging-modules/wix-log-app-info/src/main/java/com/wixpress/framework/logging/appInfo/web/LoggingAppInfoConfig.java"),
          dependencyOn("management-modules/hoopoe-management-framework/hoopoe-management-spring/src/main/java/com/wixpress/framework/health/appInfo/web/HealthAppInfoConfig.java"),
          dependencyOn("resource-pools-modules/wix-resources-app-info/src/main/scala/com/wixpress/framework/resources/statistics/AppInfoResourcesStatisticsSpringConfig.scala"),
          dependencyOn("security-modules/wix-security-management/src/main/scala/com/wixpress/framework/security/management/ManagementEndpointFilterSpringConfig.scala"),
          dependencyOn("security-modules/wix-security-web/src/main/scala/com/wixpress/framework/security/csrf/CSRFProtectionSpringConfig.scala"),
          dependencyOn("web-utils-modules/wix-http-cookies/src/main/scala/com/wixpress/framework/web/cookie/CookiesWhitelister.scala"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/spring/SpringMvcExtensionsConfig.scala"),
          dependencyOn("metrics-modules/wix-metrics-core/src/main/scala/com/wixpress/framework/monitoring/metering/appInfo/web/MetricsAppInfoConfig.java"),
          dependencyOn("metrics-modules/wix-metrics-graphite-core/src/main/scala/com/wixpress/framework/metrics/graphite/core/GraphiteMetricsObserverSpringConfig.scala"),
          dependencyOn("metrics-modules/wix-metrics-builtin/src/main/scala/com/wixpress/framework/metrics/BuiltinMetricsSpringConfig.scala"),
          dependencyOn("metrics-modules/wix-metrics-tracer-core/src/main/scala/com/wixpress/framework/metrics/tracer/core/TracerMetricsObserverSpringConfig.scala")
        )
      ),
      Code(
        codePathFrom("wix-framework-war-bootstraping/src/main/resources/wix-framework-petri-config.xml"),
        List(
          dependencyOn("petri-integration-modules/petri-integration-extensions/src/main/scala/com/wixpress/framework/petri/PetriExperimentSpecMaterializer.scala"),
          dependencyOn("petri-integration-modules/petri-integration-extensions/src/main/scala/com/wixpress/framework/petri/PetriExperimentsInScope.scala")
        )
      )
    ),
    moduleForRelativePath("bootstrap-modules/wix-server-bootstrap") -> List(
      Code(
        codePathFrom("bootstrap-modules/wix-server-bootstrap/src/main/resources/mvc-config.xml"),
        List(
          dependencyOn("bi-modules/wix-bi-context/src/main/java/com/wixpress/framework/bi/BiBeansConfig.scala"),
          dependencyOn("bi-modules/wix-bi-context/src/main/java/com/wixpress/framework/bi/WixCookieManager.java"),
          dependencyOn("web-utils-modules/wix-spring-mvc-extensions/src/main/scala/com/wixpress/framework/web/httpheaders/HttpHeadersEventHandler.java"),
          dependencyOn("validation-modules/wix-validation/src/main/java/com/wixpress/framework/validation/ValidationEventHandler.java"),
          dependencyOn("velocity-modules/hoopoe-velocity/src/main/java/com/wixpress/framework/web/velocity/WixVelocityViewResolver.java"),
          dependencyOn("throttling-modules/wix-throttling/src/main/java/com/wixpress/framework/throttling/ThrottlerRegistry.java"),
          dependencyOn("velocity-modules/hoopoe-velocity/src/main/java/com/wixpress/framework/web/velocity/TopologyTool.java"),
          dependencyOn("velocity-modules/hoopoe-velocity/src/main/java/com/wixpress/framework/topology/TopologyConfiguration.java")
        )
      )
    )
  )

  override def allCodeForModule(module: SourceModule): List[Code] = info.getOrElse(module, List.empty[Code])

  private def moduleForRelativePath(relativeModulePath: String) = sourceModules.findByRelativePath(relativeModulePath).get

  private def codePathFrom(relativeFilePath: String) = {
    val filePathParts = relativeFilePath.split('/')
    val indexOfSrc = filePathParts.indexOf("src")
    CodePath(moduleForRelativePath(filePathParts.slice(0, indexOfSrc).mkString("/")),
      filePathParts.slice(indexOfSrc, indexOfSrc + 3).mkString("/"),
      Paths.get(filePathParts.slice(indexOfSrc + 3, filePathParts.length).mkString("/")))
  }
  
  private def dependencyOn(relativeFilePath: String): Dependency =
    Dependency(codePathFrom(relativeFilePath),isCompileDependency = false)
}
