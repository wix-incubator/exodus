 ### change version of third_party that exists in your local repo

TBD

 ### change version of third_party that exists only in managed deps
  *warning* - changing the version may cause changes in transitive deps, if so, please contact bazel team

1. go to [core-server-build-tools](https://github.com/wix-private/core-server-build-tools/tree/master/third_party), locate the relevant `import_external` target in `third_party` folder. e.g.:
`third_party/eu_bitwalker.bzl`
which contains
```
load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "eu_bitwalker_UserAgentUtils",
      artifact = "eu.bitwalker:UserAgentUtils:1.19",
      jar_sha256 = "603ab4649fdc419676ed58455532af5f927c49890494ea10448e23650028936e",
  )
```

2. create the same exact file in your local repo's `third_party` folder,
3. change the version, 
4. update the sha256:
obtain the location of the jar from maven central or from artifactory.
e.g.: `http://central.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0.jar`
`https://repo.dev.wixpress.com/artifactory/libs-snapshots/org/mockito/mockito-all/1.8.3/mockito-all-1.8.3.jar`

and place the url below in place of `JAR_URL` to obtain the sha256

`wget JAR_URL -O - | openssl sha256`

3. add a load statement and a call statement to your local repo's  third_party.bzl file (located in root of repo) e.g.:
```
...

load("//:third_party/eu_bitwalker.bzl", eu_bitwalker_deps = "dependencies")

def managed_third_party_dependencies():
...

eu_bitwalker_deps()
```
