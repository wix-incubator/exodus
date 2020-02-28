load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_eclipse_jgit_org_eclipse_jgit",
      artifact = "org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r",
      artifact_sha256 = "49d912e8d5cce0dd08dca3d390189db8692a8f7e3363cdbbe182581462000aba",
      srcjar_sha256 = "b849aac124fbae572911d7ed390949cae9624badd377fef2adf105087da73c5e",
      deps = [
          "@com_googlecode_javaewah_JavaEWAH",
          "@com_jcraft_jsch",
          "@org_apache_httpcomponents_httpclient",
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES junit:
  )


  import_external(
      name = "org_eclipse_jgit_org_eclipse_jgit_http_server",
      artifact = "org.eclipse.jgit:org.eclipse.jgit.http.server:4.1.1.201511131810-r",
      artifact_sha256 = "1984842466cfcdb590f1a1263b67feac7b60b8fd36e720cca316c3c936511434",
      srcjar_sha256 = "bbd9561d5494846bde1eb79df52737a84995a18a057628b717aaf8fd82d443ea",
      deps = [
          "@org_eclipse_jgit_org_eclipse_jgit"
      ],
    # EXCLUDES junit:
  )
