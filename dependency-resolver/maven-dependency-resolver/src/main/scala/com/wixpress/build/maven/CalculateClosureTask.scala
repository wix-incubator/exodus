package com.wixpress.build.maven

import com.wixpress.build.maven.dependency.resolver.api.v1.DependenciesClosureRequest
import com.wixpress.hoopoe.ids.Guid

case class CalculateClosureTask(jobId: Guid[CalculateClosureTask], request: DependenciesClosureRequest)
