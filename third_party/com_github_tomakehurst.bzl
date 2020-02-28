load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_tomakehurst_wiremock",
      artifact = "com.github.tomakehurst:wiremock:2.9.0",
      artifact_sha256 = "55e27b0e83ded39953c937647b2bdcdb3b41b190629b6dd5621fb126888deaff",
      srcjar_sha256 = "ad70c65a1c2efe00b5dc9061cde6b676ea903372cefb59f0802f653aa767e01e",
      deps = [
          "@com_fasterxml_jackson_core_jackson_annotations",
          "@com_fasterxml_jackson_core_jackson_core",
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_flipkart_zjsonpatch_zjsonpatch",
          "@com_github_jknack_handlebars",
          "@com_google_guava_guava",
          "@com_jayway_jsonpath_json_path",
          "@junit_junit",
          "@net_sf_jopt_simple_jopt_simple",
          "@org_apache_commons_commons_lang3",
          "@org_apache_httpcomponents_httpclient",
          "@org_eclipse_jetty_jetty_server",
          "@org_eclipse_jetty_jetty_servlet",
          "@org_eclipse_jetty_jetty_servlets",
          "@org_eclipse_jetty_jetty_webapp",
          "@org_slf4j_slf4j_api",
          "@org_xmlunit_xmlunit_core",
          "@org_xmlunit_xmlunit_legacy"
      ],
  )
