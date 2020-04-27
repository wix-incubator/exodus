load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_lucene_lucene_highlighter",
      artifact = "org.apache.lucene:lucene-highlighter:7.4.0",
      artifact_sha256 = "fc7790a21d2de5148eb314b6e8c8e6cc377def0c3f4eac45caaec00368e5b303",
      srcjar_sha256 = "fb22124cca90155226159c6facff36fb393c72d5c11f1293a3add758da085930",
      excludes = [
         "org.apache.lucene:lucene-queries",
         "org.apache.lucene:lucene-join",
         "org.apache.lucene:lucene-analyzers-common",
         "org.apache.lucene:lucene-core",
         "org.apache.lucene:lucene-memory",
      ],
  )


  import_external(
      name = "org_apache_lucene_lucene_memory",
      artifact = "org.apache.lucene:lucene-memory:7.4.0",
      artifact_sha256 = "701dbfeff501f8e98b13424c8ee8f9e4eaf235af97de67d663ae2ff964d61839",
      srcjar_sha256 = "eb78f3641aaeba1a65e68868f74fe0da6c0f6aa5571478e945f92b20999e17dc",
      excludes = [
         "org.apache.lucene:lucene-core",
      ],
  )


  import_external(
      name = "org_apache_lucene_lucene_queries",
      artifact = "org.apache.lucene:lucene-queries:7.4.0",
      artifact_sha256 = "80173cb8d36119af9d55fd9f2e64cfe45d95020b69b8daebb8a6037e0e51de2b",
      srcjar_sha256 = "f8b78ed9e0c010fa8efff1fed630a8ec47dd72a142ec9d375600551277eb4728",
      excludes = [
         "org.apache.lucene:lucene-core",
      ],
  )


  import_external(
      name = "org_apache_lucene_lucene_analyzers_common",
      artifact = "org.apache.lucene:lucene-analyzers-common:7.4.0",
      artifact_sha256 = "3159322494345c4a7a3d8ea894c78afbd445ce67a3bc8a7257e12c7817e870c5",
      srcjar_sha256 = "fe71ff9a63119b7bbd0b2683640d13056479188a32855aa9163ea0f994ad4ce2",
      deps = [
          "@org_apache_lucene_lucene_core"
      ],
  )


  import_external(
      name = "org_apache_lucene_lucene_core",
      artifact = "org.apache.lucene:lucene-core:7.4.0",
      artifact_sha256 = "9afec70216c631134f73dd8661204b357963b83d36fa88c345bd756154ffb114",
      srcjar_sha256 = "f20dc2bb6cd83a8570df5cd3f254f0c2d6deb275a2d885a0f572692344a08d96",
  )


  import_external(
      name = "org_apache_lucene_lucene_queryparser",
      artifact = "org.apache.lucene:lucene-queryparser:7.4.0",
      artifact_sha256 = "f0793606c5086248fd523f213c588d0f266c852c5b878cfbf5f27a849139a1f5",
      srcjar_sha256 = "5e2ad1e5a870ef41a5a9b8c69b2e54f9c50b22ddb1c0777c47bc091e390f7453",
      deps = [
          "@org_apache_lucene_lucene_core",
          "@org_apache_lucene_lucene_queries",
          "@org_apache_lucene_lucene_sandbox"
      ],
  )


  import_external(
      name = "org_apache_lucene_lucene_sandbox",
      artifact = "org.apache.lucene:lucene-sandbox:7.4.0",
      artifact_sha256 = "825c7b194cc0280379a3354e04d16f1d768f731e94a3edae0812b4df0fd39267",
      srcjar_sha256 = "1665dae23c9c215f6a7b7a62d1c0d76e3b07f273bc76d409ae1a917f3bdac211",
      excludes = [
         "org.apache.lucene:lucene-core",
      ],
  )
