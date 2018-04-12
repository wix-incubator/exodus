import argparse
import os.path
import subprocess

import sys

CI_ENV_FLAG_FILE = "/tools/ci.environment"

parser = argparse.ArgumentParser()
parser.add_argument('workspace_dir')
args = parser.parse_args()

workspace_dir = args.workspace_dir
commits_bzl_file = workspace_dir + "/tools/commits.bzl"


def file_is_not_empty(file):
    return os.stat(file).st_size != 0


if (os.path.isfile(commits_bzl_file) and file_is_not_empty(commits_bzl_file)) and \
        (not os.path.isfile(workspace_dir + CI_ENV_FLAG_FILE)):
    sys.exit(0)

repos = {"core_server_build_tools": "git@github.com:wix-private/core-server-build-tools.git"}

last_commits = {}

for (repo, repo_url) in repos.items():
    commits_output = subprocess.check_output(['git', 'ls-remote', '--heads', repo_url, 'refs/heads/master'])
    last_commit = commits_output.decode("utf-8").splitlines()[0].split('\t')[0]
    last_commits[repo] = last_commit

with open(commits_bzl_file,"w+") as repo_commits:
    repo_commits.write("REPO_COMMITS = {\n")
    for (repo, value_of_last_commit) in last_commits.items():
        placeholder_token = "%s_commit" % repo
        repo_commits.write("'%s' : '%s'\n" % (placeholder_token, value_of_last_commit))
    repo_commits.write("}\n")

open(workspace_dir + "/BUILD.bazel", 'a').close()
open(workspace_dir + "/tools/BUILD.bazel", 'a').close()