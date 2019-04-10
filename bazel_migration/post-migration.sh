#!/bin/bash

git reset HEAD workspaces-resolution/ e2e/ bazel_assembly/ wix-config/ wix-docker/ wix-kube/ external_third_parties/ wazel/installer/ exploration_tests/manual templates/ depfixer/ migrator/wix-bazel-migrator
git checkout -- workspaces-resolution/ e2e/ bazel_assembly/ wix-config/ wix-docker/ wix-kube/ external_third_parties/ wazel/installer/ exploration_tests/manual templates/ depfixer/ migrator/wix-bazel-migrator

buildozer 'set flaky 'True'' //dependency-synchronizer/bazel-deps-synchronizer-webapp/src/it/scala/com/wixpress/build/sync/e2e:e2e
buildozer 'set flaky 'True'' //wix-bazel-repositories/bazel-repositories-server/src/it/scala/com/wixpress/build/bazel/repositories:repositories

# bootstrap modules
${commons}/bootstrapify --label //dependency-synchronizer/bazel-deps-synchronizer-webapp/src/main/scala/com/wixpress/build/sync:sync --group_id com.wixpress.build --artifact_id bazel-deps-synchronizer-webapp --main_class com.wixpress.build.sync.BazelMavenSynchronizerServer
${commons}/bootstrapify --label //wix-bazel-repositories/bazel-repositories-server/src/main/scala/com/wixpress/build/bazel/repositories:repositories --group_id com.wixpress.build --artifact_id bazel-repositories-server --main_class com.wixpress.build.bazel.repositories.BazelRepositoriesServer

# for second party source dependnecies whitelist gen
buildozer 'new java_binary write_whitelist' //repo-analyzer/wix-maven-modules/src/main/scala/com/wixpress/build:__pkg__
buildozer 'set main_class com.wixpress.build.VCSMavenModulesCli' //repo-analyzer/wix-maven-modules/src/main/scala/com/wixpress/build:write_whitelist
buildozer 'add runtime_deps :build' //repo-analyzer/wix-maven-modules/src/main/scala/com/wixpress/build:write_whitelist

# for migrator build job
buildozer 'new java_binary migrator_cli' //migrator/wix-bazel-migrator:__pkg__
buildozer 'set main_class com.wix.bazel.migrator.Migrator' //migrator/wix-bazel-migrator:migrator_cli
buildozer 'add runtime_deps //migrator/wix-bazel-migrator/src/main/java/com/wix:agg=bazel/migrator_bazel/migrator/tinker_bazel/migrator/transform_build/maven/analysis' //migrator/wix-bazel-migrator:migrator_cli

buildozer 'new java_binary sync_cli' //dependency-synchronizer/bazel-deps-synchronizer-cli/src/main/scala/com/wix/build/sync:__pkg__
buildozer 'set main_class com.wix.build.sync.DependencySynchronizerCli' //dependency-synchronizer/bazel-deps-synchronizer-cli/src/main/scala/com/wix/build/sync:sync_cli
buildozer 'add runtime_deps //dependency-synchronizer/bazel-deps-synchronizer-cli/src/main/scala/com/wix/build/sync' //dependency-synchronizer/bazel-deps-synchronizer-cli/src/main/scala/com/wix/build/sync:sync_cli

buildozer 'new java_binary fw_sync_cli' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:__pkg__
buildozer 'set main_class com.wix.build.sync.snapshot.FWDependenciesSynchronizerCli' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:fw_sync_cli
buildozer 'add runtime_deps //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:fw_sync_cli

buildozer 'new java_binary snapshot_to_single_repo_sync_cli' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:__pkg__
buildozer 'set main_class com.wix.build.sync.snapshot.SnapshotsToSingleRepoSynchronizerCli' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:snapshot_to_single_repo_sync_cli
buildozer 'add runtime_deps //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot' //dependency-synchronizer/bazel-fw-deps-synchronizer-cli/src/main/scala/com/wix/build/sync/snapshot:snapshot_to_single_repo_sync_cli
