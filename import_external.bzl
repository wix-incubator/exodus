load("@io_bazel_rules_scala//scala:scala_maven_import_external.bzl", "scala_maven_import_external", "scala_import_external")

_default_server_urls = ["https://repo.maven.apache.org/maven2/",
                        "https://mvnrepository.com/artifact",
                        "https://maven-central.storage.googleapis.com",
                        "http://gitblit.github.io/gitblit-maven",]

def safe_wix_scala_maven_import_external(name, artifact, **kwargs):
  if native.existing_rule(name) == None:
        wix_scala_maven_import_external(
            name = name,
            artifact = artifact,
            **kwargs
        )


def wix_scala_maven_import_external(name, artifact, **kwargs):
  fetch_sources = kwargs.get("srcjar_sha256") != None
  wix_scala_maven_import_external_sources(name, artifact, fetch_sources, **kwargs)

def wix_snapshot_scala_maven_import_external(name, artifact, **kwargs):
  wix_scala_maven_import_external_sources(name, artifact, True, **kwargs)
  
def wix_scala_maven_import_external_sources(name, artifact, fetch_sources, **kwargs):
  tags = kwargs.pop('tags',[])
  excludes = kwargs.pop('excludes',[])
  if (len(excludes)):
      tags = ["excludes=%s"% ",".join(excludes)] + tags
  scala_maven_import_external(
      name = name,
      artifact = artifact,
      licenses = ["notice"],  # Apache 2.0
      fetch_sources = fetch_sources,
      server_urls = _default_server_urls,
      tags = ["maven_coordinates=%s"%artifact, ] + tags,
      **kwargs
  )