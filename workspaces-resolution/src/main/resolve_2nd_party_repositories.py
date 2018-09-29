import argparse
import json
import os
import os.path
import subprocess
import sys
import logging

logging_level = logging.DEBUG if "DEBUG_2ND_PARTY_SCRIPT" in os.environ else logging.INFO

logging.basicConfig(level=logging_level, format='%(asctime)s  %(levelname)s: %(message)s')

from StringIO import StringIO

if sys.version_info[0] == 3:
    from urllib.request import urlopen
else:
    # Not Python 3 - today, it is most likely to be Python 2
    # But note that this might need an update when Python 4
    # might be around one day
    from urllib import urlopen

tools_relative_path = "/tools/"
CI_ENV_FLAG_FILE = tools_relative_path + "ci.environment"
starlark_file_name_postfix = "_2nd_party_resolved_dependencies.bzl"
json_file_name_postfix = "_2nd_party_resolved_dependencies.json"
symlink_relative_path = tools_relative_path + "2nd_party_resolved_dependencies_current_branch.bzl"

repo_list = os.environ.get("REPO_LIST", "default")
tracking_branch = os.environ.get("TRACKING_BRANCH", "master")

repositories_url = os.environ.get("REPOSITORIES_URL", "https://bo.wix.com/bazel-repositories-server/repositories")
url_with_list = repositories_url + ("?list=%s" % repo_list)


def fetch_repositories():
    response = urlopen(url_with_list).read()
    io = StringIO()
    json.dump(json.loads(response)["repositories"], io, sort_keys=True, indent=4, separators=(',', ': '))
    json_file_repos = io.getvalue()
    starlark_file_repos = json.loads(response)["resolvedDependenciesFile"]
    return json_file_repos, starlark_file_repos


def file_is_not_empty(some_file):
    return os.stat(some_file).st_size != 0


def read_current_repo_url():
    return run_process(['git', 'config', '--get', 'remote.origin.url'],
                       "Failed to read the current repository remote origin url")


def read_current_branch():
    return run_process(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], "Failed to read the current branch")


def write_symlink_to_path(symlink_path, path):
    run_process(['ln', '-sf', path, symlink_path], "Failed to write symlink %s => %s" % (symlink_path, path))


def run_process(splitted_command, fail_msg):
    logging.debug("Running:\t%s" % ' '.join(splitted_command))
    process = subprocess.Popen(splitted_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = process.communicate()
    if err:
        msg = "%s. stderr = %s" % (fail_msg, err)
        logging.error(msg)
        raise Exception(msg)
    logging.debug("Output:\t%s" % out)
    return out


def write_content_to_path(content, path):
    with open(path, "w+") as openedfile:
        logging.debug("Generating %s with content:\n%s" % (path, content))
        openedfile.write(content)
        openedfile.close()


def write_repositories(workspace_dir):
    current_branch = read_current_branch()
    starlark_file_path = (workspace_dir + tools_relative_path + current_branch + starlark_file_name_postfix).replace(
        "\n", "")
    if (os.path.isfile(starlark_file_path) and file_is_not_empty(starlark_file_path)) and (
            not os.path.isfile(workspace_dir + CI_ENV_FLAG_FILE)):
        sys.exit(0)

    logging.debug("Fetching repositories from %s" % url_with_list)
    json_file_repos, starlark_file_repos = fetch_repositories()

    json_file_path = (workspace_dir + tools_relative_path + current_branch + json_file_name_postfix).replace("\n", "")
    symlink_path = workspace_dir + symlink_relative_path
    write_content_to_path(json_file_repos, json_file_path)
    write_content_to_path(starlark_file_repos, starlark_file_path)
    write_symlink_to_path(symlink_path, starlark_file_path)

    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()


def parse_workspace_dir():
    parser = argparse.ArgumentParser()
    parser.add_argument('workspace_dir')
    args = parser.parse_args()
    return args.workspace_dir


def main():
    write_repositories(parse_workspace_dir())


if __name__ == "__main__":
    main()
