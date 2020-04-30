load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven","parse")


def dependency(coordinates,exclusions=None):
    artifact = parse.parse_maven_coordinate(coordinates)
    return maven.artifact(
            group =  artifact['group'],
            artifact = artifact['artifact'],
            packaging =  artifact.get('packaging'),
            classifier = artifact.get('classifier'),
            version =  artifact['version'],
            exclusions = exclusions,
        )


deps = [
    dependency("args4j:args4j:2.0.29"),
    dependency("backport-util-concurrent:backport-util-concurrent:3.1"),
    dependency("ch.qos.logback:logback-classic:1.1.11"),
    dependency("ch.qos.logback:logback-core:1.1.11"),
    dependency("com.chuusai:shapeless_2.12:2.3.3"),
    dependency("com.codota:codota-sdk-java:1.0.11"),
    dependency("com.fasterxml.jackson.core:jackson-annotations:2.9.6"),
    dependency("com.fasterxml.jackson.core:jackson-core:2.9.6"),
    dependency("com.fasterxml.jackson.core:jackson-databind:2.9.6"),
    dependency("com.fasterxml.jackson.module:jackson-module-paranamer:2.9.6"),
    dependency("com.fasterxml.jackson.module:jackson-module-scala_2.12:2.9.6"),
    dependency("com.flipkart.zjsonpatch:zjsonpatch:0.3.0"),
    dependency("com.force.api:force-partner-api:24.0.0"),
    dependency("com.force.api:force-wsc:24.0.0"),
    dependency("com.gitblit:gitblit:1.8.0", exclusions = ["org.eclipse.jetty.aggregate:jetty-all"]),
    dependency("com.github.dblock.waffle:waffle-jna:1.7.3"),
    dependency("com.github.jknack:handlebars:4.0.6", exclusions = ["org.mozilla:rhino"]),
    dependency("com.github.marschall:memoryfilesystem:1.2.0"),
    dependency("com.github.pathikrit:better-files_2.12:2.17.1"),
    dependency("com.github.scopt:scopt_2.12:3.7.0"),
    dependency("com.github.tomakehurst:wiremock:2.9.0"),
    dependency("com.google.code.findbugs:jsr305:3.0.2"),
    dependency("com.google.code.gson:gson:2.8.5"),
    dependency("com.google.collections:google-collections:1.0"),
    dependency("com.google.errorprone:error_prone_annotations:2.2.0"),
    dependency("com.google.guava:guava:25.1-jre"),
    dependency("com.google.inject.extensions:guice-servlet:4.2.3"),
    dependency("com.google.inject:guice:4.0"),
    dependency("com.google.inject:guice:jar:no_aop:4.2.0"),
    dependency("com.google.j2objc:j2objc-annotations:1.1"),
    dependency("com.googlecode.javaewah:JavaEWAH:1.1.6"),
    dependency("com.intellij:annotations:12.0"),
    dependency("com.jayway.jsonpath:json-path:2.4.0"),
    dependency("com.jcraft:jsch:0.1.54"),
    dependency("com.sun.mail:javax.mail:1.5.1"),
    dependency("com.thoughtworks.paranamer:paranamer:2.8"),
    dependency("com.toedter:jcalendar:1.3.2"),
    dependency("com.typesafe.akka:akka-actor_2.12:2.5.15"),
    dependency("com.typesafe.akka:akka-http_2.12:10.1.4"),
    dependency("com.typesafe.akka:akka-http-core_2.12:10.1.4"),
    dependency("com.typesafe.akka:akka-parsing_2.12:10.1.4"),
    dependency("com.typesafe.akka:akka-protobuf_2.12:2.5.15"),
    dependency("com.typesafe.akka:akka-stream_2.12:2.5.15"),
    dependency("com.typesafe:config:1.3.3"),
    dependency("com.typesafe:ssl-config-core_2.12:0.2.4"),
    dependency("com.unboundid:unboundid-ldapsdk:2.3.8"),
    dependency("com.wix:http-testkit_2.12:0.1.19"),
    dependency("com.wix:http-testkit-client_2.12:0.1.19"),
    dependency("com.wix:http-testkit-core_2.12:0.1.19"),
    dependency("com.wix:http-testkit-server_2.12:0.1.19"),
    dependency("com.wix:http-testkit-specs2_2.12:0.1.19"),
    dependency("commons-codec:commons-codec:1.11"),
    dependency("commons-collections:commons-collections:3.2.2"),
    dependency("commons-io:commons-io:2.6"),
    dependency("commons-lang:commons-lang:2.6"),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("dom4j:dom4j:1.6.1", exclusions = ["xml-apis:xml-apis"]),
    dependency("javax.activation:activation:1.1"),
    dependency("javax.annotation:jsr250-api:1.0"),
    dependency("javax.enterprise:cdi-api:1.0", exclusions = ["javax.el:el-api", "org.jboss.ejb3:jboss-ejb3-api", "org.jboss.interceptor:jboss-interceptor-api"]),
    dependency("javax.inject:javax.inject:1"),
    dependency("javax.servlet:javax.servlet-api:3.1.0"),
    dependency("jdom:jdom:1.0"),
    dependency("joda-time:joda-time:2.10"),
    dependency("junit:junit:4.12", exclusions = ["org.hamcrest:hamcrest-core"]),
    dependency("log4j:log4j:1.2.17"),
    dependency("net.bytebuddy:byte-buddy:1.8.15"),
    dependency("net.bytebuddy:byte-buddy-agent:1.8.15"),
    dependency("net.java.dev.jna:jna:4.5.2"),
    dependency("net.java.dev.jna:jna-platform:4.5.2"),
    dependency("net.minidev:accessors-smart:1.2"),
    dependency("net.minidev:json-smart:2.3"),
    dependency("net.sf.jopt-simple:jopt-simple:5.0.4"),
    dependency("net.sourceforge.jchardet:jchardet:1.0"),
    dependency("org.antlr:antlr4-runtime:4.5.1-1", exclusions = ["org.abego.treelayout:org.abego.treelayout.core"]),
    dependency("org.apache.commons:commons-collections4:4.1"),
    dependency("org.apache.commons:commons-compress:1.18"),
    dependency("org.apache.commons:commons-lang3:3.8.1"),
    dependency("org.apache.commons:commons-pool2:2.6.0"),
    dependency("org.apache.httpcomponents:httpclient:4.5.6", exclusions = ["commons-logging:commons-logging"]),
    dependency("org.apache.httpcomponents:httpcore:4.4.10"),
    dependency("org.apache.ivy:ivy:2.2.0"),
    dependency("org.apache.lucene:lucene-analyzers-common:7.4.0"),
    dependency("org.apache.lucene:lucene-core:7.4.0"),
    dependency("org.apache.lucene:lucene-highlighter:7.4.0", exclusions = ["org.apache.lucene:lucene-queries", "org.apache.lucene:lucene-join", "org.apache.lucene:lucene-analyzers-common", "org.apache.lucene:lucene-core", "org.apache.lucene:lucene-memory"]),
    dependency("org.apache.lucene:lucene-memory:7.4.0", exclusions = ["org.apache.lucene:lucene-core"]),
    dependency("org.apache.lucene:lucene-queries:7.4.0", exclusions = ["org.apache.lucene:lucene-core"]),
    dependency("org.apache.lucene:lucene-queryparser:7.4.0"),
    dependency("org.apache.lucene:lucene-sandbox:7.4.0", exclusions = ["org.apache.lucene:lucene-core"]),
    dependency("org.apache.maven.archetype:archetype-catalog:2.2"),
    dependency("org.apache.maven.archetype:archetype-common:2.2"),
    dependency("org.apache.maven.archetype:archetype-descriptor:2.2"),
    dependency("org.apache.maven.archetype:archetype-registry:2.2"),
    dependency("org.apache.maven:maven-artifact:3.5.4"),
    dependency("org.apache.maven:maven-artifact-manager:2.2.1"),
    dependency("org.apache.maven:maven-builder-support:3.5.4"),
    dependency("org.apache.maven:maven-core:3.5.4"),
    dependency("org.apache.maven:maven-model:3.5.4"),
    dependency("org.apache.maven:maven-model-builder:3.5.4"),
    dependency("org.apache.maven:maven-plugin-api:3.5.4"),
    dependency("org.apache.maven:maven-plugin-registry:2.2.1"),
    dependency("org.apache.maven:maven-profile:2.2.1"),
    dependency("org.apache.maven:maven-project:2.2.1"),
    dependency("org.apache.maven:maven-repository-metadata:3.5.4"),
    dependency("org.apache.maven:maven-resolver-provider:3.5.4"),
    dependency("org.apache.maven:maven-settings:3.5.4"),
    dependency("org.apache.maven:maven-settings-builder:3.5.4"),
    dependency("org.apache.maven.resolver:maven-resolver-api:1.3.3"),
    dependency("org.apache.maven.resolver:maven-resolver-connector-basic:1.3.1"),
    dependency("org.apache.maven.resolver:maven-resolver-impl:1.3.3"),
    dependency("org.apache.maven.resolver:maven-resolver-spi:1.3.3"),
    dependency("org.apache.maven.resolver:maven-resolver-transport-file:1.3.1"),
    dependency("org.apache.maven.resolver:maven-resolver-transport-http:1.3.1"),
    dependency("org.apache.maven.resolver:maven-resolver-util:1.3.1"),
    dependency("org.apache.maven.shared:maven-common-artifact-filters:1.4"),
    dependency("org.apache.maven.shared:maven-invoker:2.0.11"),
    dependency("org.apache.maven.shared:maven-shared-utils:3.2.1"),
    dependency("org.apache.maven.wagon:wagon-provider-api:1.0-beta-6"),
    dependency("org.apache.mina:mina-core:2.0.9", exclusions = ["org.easymock:*"]),
    dependency("org.apache.sshd:sshd-common:2.1.0"),
    dependency("org.apache.sshd:sshd-core:2.1.0", exclusions = ["org.easymock:*"]),
    dependency("org.apache.tika:tika-core:1.5"),
    dependency("org.apache.velocity:velocity:1.7"),
    dependency("org.apache.wicket:wicket:1.4.22", exclusions = ["org.mockito:*"]),
    dependency("org.apache.wicket:wicket-auth-roles:1.4.22", exclusions = ["org.mockito:*"]),
    dependency("org.apache.wicket:wicket-extensions:1.4.22", exclusions = ["org.mockito:*"]),
    dependency("org.apache.xbean:xbean-reflect:3.7"),
    dependency("org.bouncycastle:bcmail-jdk15on:1.52"),
    dependency("org.bouncycastle:bcpkix-jdk15on:1.57"),
    dependency("org.bouncycastle:bcprov-jdk15on:1.60"),
    dependency("org.checkerframework:checker-qual:2.0.0"),
    dependency("org.codehaus.groovy:groovy-all:2.4.4"),
    dependency("org.codehaus.mojo:animal-sniffer-annotations:1.14"),
    dependency("org.codehaus.mojo:mrm-api:1.1.0"),
    dependency("org.codehaus.mojo:mrm-maven-plugin:1.1.0", exclusions = ["org.mortbay.jetty:servlet-api"]),
    dependency("org.codehaus.mojo:mrm-servlet:1.1.0", exclusions = ["org.mortbay.jetty:servlet-api"]),
    dependency("org.codehaus.plexus:plexus-archiver:3.4"),
    dependency("org.codehaus.plexus:plexus-classworlds:2.5.2"),
    dependency("org.codehaus.plexus:plexus-component-annotations:1.7.1", exclusions = ["junit:junit"]),
    dependency("org.codehaus.plexus:plexus-container-default:1.7.1"),
    dependency("org.codehaus.plexus:plexus-interpolation:1.24"),
    dependency("org.codehaus.plexus:plexus-io:2.7.1"),
    dependency("org.codehaus.plexus:plexus-utils:3.1.0"),
    dependency("org.codehaus.plexus:plexus-velocity:1.1.8"),
    dependency("org.eclipse.jetty:jetty-continuation:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-http:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-io:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-security:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-server:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-servlet:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-servlets:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-util:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-webapp:9.4.11.v20180605"),
    dependency("org.eclipse.jetty:jetty-xml:9.4.11.v20180605"),
    dependency("org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r", exclusions = ["junit:*"]),
    dependency("org.eclipse.jgit:org.eclipse.jgit.http.server:4.1.1.201511131810-r", exclusions = ["junit:*"]),
    dependency("org.eclipse.sisu:org.eclipse.sisu.inject:0.3.3"),
    dependency("org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.3"),
    dependency("org.freemarker:freemarker:2.3.22"),
    dependency("org.fusesource.wikitext:confluence-core:1.4"),
    dependency("org.fusesource.wikitext:mediawiki-core:1.4"),
    dependency("org.fusesource.wikitext:textile-core:1.4"),
    dependency("org.fusesource.wikitext:tracwiki-core:1.4"),
    dependency("org.fusesource.wikitext:twiki-core:1.4"),
    dependency("org.fusesource.wikitext:wikitext-core:1.4"),
    dependency("org.hamcrest:hamcrest-core:1.3"),
    dependency("org.iq80.snappy:snappy:0.4"),
    dependency("org.javassist:javassist:3.22.0-GA"),
    dependency("org.jetbrains:annotations:13.0"),
    dependency("org.jgrapht:jgrapht-core:0.9.2"),
    dependency("org.joda:joda-convert:2.1.1"),
    dependency("org.jsoup:jsoup:1.11.3"),
    dependency("org.kohsuke:libpam4j:1.8"),
    dependency("org.mockito:mockito-core:2.21.0", exclusions = ["org.hamcrest:hamcrest-all", "org.hamcrest:hamcrest-core"]),
    dependency("org.mortbay.jetty:jetty:6.1.26"),
    dependency("org.mortbay.jetty:jetty-util:6.1.26"),
    dependency("org.mortbay.jetty:servlet-api:2.5-20081211"),
    dependency("org.objenesis:objenesis:3.0"),
    dependency("org.ow2.asm:asm:6.2.1"),
    dependency("org.ow2.asm:asm-analysis:6.2.1"),
    dependency("org.ow2.asm:asm-tree:6.2.1"),
    dependency("org.ow2.asm:asm-util:5.0.3"),
    dependency("org.parboiled:parboiled-core:1.1.7"),
    dependency("org.parboiled:parboiled-java:1.1.7"),
    dependency("org.pegdown:pegdown:1.5.0"),
    dependency("org.reactivestreams:reactive-streams:1.0.2"),
    dependency("org.reflections:reflections:0.9.11"),
    dependency("org.scala-lang.modules:scala-java8-compat_2.12:0.8.0"),
    dependency("org.scala-lang.modules:scala-parser-combinators_2.12:1.0.4"),
    dependency("org.scala-lang.modules:scala-xml_2.12:1.1.0"),
    dependency("org.scala-lang:scala-compiler:2.12.6"),
    dependency("org.scala-lang:scala-library:2.12.6"),
    dependency("org.scala-lang:scala-reflect:2.12.6"),
    dependency("org.scala-sbt:test-interface:1.0"),
    dependency("org.scalaj:scalaj-http_2.12:2.4.1"),
    dependency("org.slf4j:jcl-over-slf4j:1.7.25"),
    dependency("org.slf4j:slf4j-api:1.7.25"),
    dependency("org.slf4j:slf4j-log4j12:1.7.12"),
    dependency("org.sonatype.plexus:plexus-cipher:1.4"),
    dependency("org.sonatype.plexus:plexus-sec-dispatcher:1.4"),
    dependency("org.sonatype.sisu:sisu-guice:jar:no_aop:3.1.6", exclusions = ["com.google.code.findbugs:jsr305", "aopalliance:aopalliance"]),
    dependency("org.specs2:classycle:1.4.3"),
    dependency("org.specs2:specs2-analysis_2.12:4.4.1"),
    dependency("org.specs2:specs2-common_2.12:4.4.1"),
    dependency("org.specs2:specs2-core_2.12:4.4.1"),
    dependency("org.specs2:specs2-fp_2.12:4.4.1"),
    dependency("org.specs2:specs2-junit_2.12:4.4.1"),
    dependency("org.specs2:specs2-matcher_2.12:4.4.1"),
    dependency("org.specs2:specs2-matcher-extra_2.12:4.4.1"),
    dependency("org.specs2:specs2-mock_2.12:4.4.1"),
    dependency("org.specs2:specs2-shapeless_2.12:4.4.1"),
    dependency("org.tukaani:xz:1.5"),
    dependency("org.typelevel:macro-compat_2.12:1.1.1"),
    dependency("org.xmlunit:xmlunit-core:2.3.0"),
    dependency("org.xmlunit:xmlunit-legacy:2.3.0"),
    dependency("oro:oro:2.0.8"),
    dependency("redis.clients:jedis:2.9.0"),
    dependency("rhino:js:1.7R2"),
    dependency("ro.fortsoft.pf4j:pf4j:0.9.0"),
    dependency("rome:rome:0.9"),
    dependency("rome:rome:0.9"),
    dependency("velocity:velocity:1.5"),


]

def dependencies():
    maven_install(
        artifacts = deps,
        repositories = [
            "https://repo.maven.apache.org/maven2/",
            "https://mvnrepository.com/artifact",
            "https://maven-central.storage.googleapis.com",
            "http://gitblit.github.io/gitblit-maven",
            ],
        generate_compat_repositories = True,
        # bazel 'run @maven//:pin' to acquire this json file
        maven_install_json = "//:maven_install.json",
    )
