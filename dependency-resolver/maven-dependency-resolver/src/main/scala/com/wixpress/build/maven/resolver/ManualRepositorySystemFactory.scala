package com.wixpress.build.maven.resolver

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory


/**
  * A factory for repository system instances that employs Maven Artifact Resolver's built-in service locator
  * infrastructure to wire up the system's components.
  */
object ManualRepositorySystemFactory {
  def newRepositorySystem: RepositorySystem = {
    /*
     * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
     * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
     * factories.
     */
    val locator = MavenRepositorySystemUtils.newServiceLocator
    locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      override def serviceCreationFailed(`type`: Class[_], impl: Class[_], exception: Throwable): Unit = {
        exception.printStackTrace()
      }
    })
    locator.getService(classOf[RepositorySystem])
  }
}
