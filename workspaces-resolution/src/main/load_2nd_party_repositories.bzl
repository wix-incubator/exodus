load("//tools:2nd_party_resolved_dependencies_current_branch.bzl", "resolved")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def load_2nd_party_repositories():
    for repo in resolved:
        if repo["rule_class"] == "@bazel_tools//tools/build_defs/repo:git.bzl%git_repository" and repo["attributes"]["name"] != "server_infra":
            git_repository(**(repo["attributes"]))
