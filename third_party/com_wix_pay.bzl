load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_wix_pay_credit_card_networks",
      artifact = "com.wix.pay:credit-card-networks:1.5.0-SNAPSHOT",
      deps = [
          "@org_scala_lang_scala_reflect"
      ],
  )
