package com.wix.bazel.migrator

import java.io.File
import java.nio.file.{Files, Path, Paths}


case class RunConfiguration(repoRoot: File,
                            repoUrl: String,
                            managedDepsRepo: File,
                            codotaToken: String,
                            performMavenClasspathResolution: Boolean = true,
                            performTransformation: Boolean = true,
                            failOnSevereConflicts: Boolean = false,
                            interRepoSourceDependency: Boolean = false,
                            artifactoryToken: Option[String] = None,
                            sourceDependenciesWhitelist: Option[Path] = None,
                            additionalDepsByMavenDeps: Option[Path] = None,
                            includeServerInfraInSocialModeSet: Boolean = false,
                            m2Path: Option[Path] = None,
                            thirdPartyDependenciesSource: Option[String] = None)

object RunConfiguration {
  private val Empty = RunConfiguration(null, null, null, null)

  private val parser = new scopt.OptionParser[RunConfiguration]("Migrator") {
    head("Wix Bazel Migrator")

    opt[String]('r', "repo")
      .required()
      .withFallback(() => sys.props.get("repo.root")
        .getOrElse(throw new IllegalArgumentException("no repo root defined")))
      .validate(f => if (Files.isDirectory(Paths.get(f))) success else failure(s"repo $f must be existing directory"))
      .action { case (f, cfg) => cfg.copy(repoRoot = new File(f)) }

    opt[String]('m', "managed-deps-repo")
      .required()
      .withFallback(() => sys.props.get("managed.deps.repo")
        .getOrElse(throw new IllegalArgumentException("no managed deps repo defined")))
      .validate(f => if (Files.isDirectory(Paths.get(f))) success else failure(s"repo $f must be existing directory"))
      .action { case (f, cfg) => cfg.copy(managedDepsRepo = new File(f)) }

    opt[String]("repo-git-url")
      .required()
      .withFallback(() => sys.props.get("repo.url").orElse(sys.env.get("repo_url"))
        .getOrElse(throw new IllegalArgumentException("no repository git url defined")))
      .validate(url => if (url.startsWith("git@") && url.endsWith(".git")) success else failure(s"$url must be valid git url"))
      .action { case (url, cfg) => cfg.copy(repoUrl = url) }

    opt[String]("codota-token")
      .required()
      .withFallback(() => sys.props.get("codota.token")
        .getOrElse(throw new IllegalArgumentException("no codota token defined")))
      .action { case (token, cfg) => cfg.copy(codotaToken = token) }

    opt[Boolean]("skip-maven")
      .required()
      .withFallback(() => booleanProperty("skip.classpath"))
      .action { case (skip, cfg) => cfg.copy(performMavenClasspathResolution = !skip) }

    opt[Boolean]("skip-transform")
      .required()
      .withFallback(() => booleanProperty("skip.transformation"))
      .action { case (skip, cfg) => cfg.copy(performTransformation = !skip) }

    opt[Boolean]("fail-on-severe-conflicts")
      .required()
      .withFallback(() => booleanProperty("fail.on.severe.conflicts"))
      .action { case (fail, cfg) => cfg.copy(failOnSevereConflicts = fail) }

    opt[Boolean]("inter-repo-source-dependency")
      .withFallback(() => booleanProperty("inter.repo.source.dependency"))
      .action { case (interRepoSourceDependency, cfg) => cfg.copy(interRepoSourceDependency = interRepoSourceDependency) }

    opt[String]("artifactory-token")
      .required()
      .withFallback(() => sys.props.getOrElse("artifactory.token", ""))
      .action {
        case (token, cfg) if Option(token).exists(_.nonEmpty) => cfg.copy(artifactoryToken = Some(token))
        case (_, cfg) =>  cfg.copy(artifactoryToken = None)
      }

    opt[String]("source-dependencies-whitelist")
      .withFallback(() => sys.props.getOrElse("source.dependencies.whitelist", ""))
      .action { case (path, cfg) if path != "" => cfg.copy(sourceDependenciesWhitelist = Some(Paths.get(path)))
                case (_, cfg) =>  cfg.copy(sourceDependenciesWhitelist = None)}

    opt[String]("additional-deps-by-maven-deps")
      .withFallback(() => sys.props.getOrElse("additional.deps.by.maven.deps", ""))
      .action { case (path, cfg) if path != "" => cfg.copy(additionalDepsByMavenDeps = Some(Paths.get(path)))
      case (_, cfg) =>  cfg.copy(additionalDepsByMavenDeps = None)}

    opt[Boolean]("include-server-infra-in-social-mode-set")
      .withFallback(() => booleanProperty("include.server.infra.in.social.mode.set"))
      .action { case (include, cfg) => cfg.copy(includeServerInfraInSocialModeSet = include) }

    opt[String]("local-maven-repository-path")
      .withFallback(() => sys.props.getOrElse("local.maven.repository.path", ""))
      .action {
        case (path, cfg) if Option(path).exists(_.nonEmpty) => cfg.copy(m2Path = Some(Paths.get(path).toAbsolutePath))
        case (_, cfg) =>  cfg.copy(m2Path = None)
      }

    opt[String]("third-party-dependencies-source")
      .withFallback(() => sys.props.getOrElse("third.party.dependencies.source", ""))
      .action {
        case (coords, cfg) if Option(coords).exists(_.nonEmpty) => cfg.copy(thirdPartyDependenciesSource = Some(coords))
        case (_, cfg) =>  cfg.copy(thirdPartyDependenciesSource = None)
      }
  }

  private def booleanProperty(prop: String) = sys.props.get(prop).exists(_.toBoolean)

  def from(cliArgs: Array[String]): RunConfiguration = {
    parser.parse(cliArgs, Empty).getOrElse(sys.exit(-1))
  }
}
