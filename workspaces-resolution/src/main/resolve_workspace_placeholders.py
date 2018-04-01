import argparse
import os.path
import subprocess

import sys

TEMPLATE_NAME = "WORKSPACE.template"

parser = argparse.ArgumentParser()
parser.add_argument('workspace_dir')
args = parser.parse_args()

workspace_dir = args.workspace_dir

workspace_path = workspace_dir + "/WORKSPACE"
if os.path.isfile(workspace_path) and os.stat(workspace_path).st_size != 0:
    sys.exit(0)

template_path = workspace_dir + "/" + TEMPLATE_NAME
if os.path.isfile(template_path):
    template_file_already_resolved = True
else:
    raise ValueError("template file ({}) is missing".format(TEMPLATE_NAME))

repos = {"core_server_build_tools": "git@github.com:wix-private/core-server-build-tools.git"}

last_commits = {}

for (repo, repo_url) in repos.items():
    commits_output = subprocess.check_output(['git', 'ls-remote', '--heads', repo_url, 'refs/heads/master'])
    last_commit = commits_output.decode("utf-8").splitlines()[0].split('\t')[0]
    last_commits[repo] = last_commit


with open(template_path) as template_file:
    template_content = template_file.read()

for (repo, value_of_last_commit) in last_commits.items():
    escaped_repo_name = repo.replace("-","_").replace("/","_")
    placeholder_token = "%s_commit" % escaped_repo_name
    template_content = template_content.replace(placeholder_token, "\"%s\"" % value_of_last_commit, 1)

with open("WORKSPACE", 'w') as output_workspace_file:
    output_workspace_file.write(template_content)
