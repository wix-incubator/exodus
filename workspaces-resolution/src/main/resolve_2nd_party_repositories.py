import argparse
import json
import os
import os.path
import subprocess
import sys
import logging
import base64
import socket
try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO

logging_level = logging.DEBUG if "DEBUG_2ND_PARTY_SCRIPT" in os.environ else logging.INFO

logging.basicConfig(level=logging_level, format='%(asctime)s  %(levelname)s: %(message)s')

if sys.version_info[0] == 3:
    from urllib.request import urlopen
    from urllib.error import URLError
else:
    # Not Python 3 - today, it is most likely to be Python 2
    # But note that this might need an update when Python 4
    # might be around one day
    from urllib2 import urlopen
    from urllib2 import URLError

tools_relative_path = "/tools/"
starlark_file_name_postfix = "_2nd_party_resolved_dependencies.bzl"
json_file_name_postfix = "_2nd_party_resolved_dependencies.json"
symlink_relative_path = tools_relative_path + "2nd_party_resolved_dependencies_current_branch.bzl"
second_party_resolved_dependencies_empty_placeholder = "EMPTY!"
second_party_resolved_dependencies_env_var_name = "SECOND_PARTY_RESOLVED_DEPENDENCIES"

repo_list = os.environ.get("REPO_LIST", "default")
tracking_branch = os.environ.get("TRACKING_BRANCH", "master")
second_party_resolved_dependencies = os.environ.get(second_party_resolved_dependencies_env_var_name)

repositories_url = os.environ.get("REPOSITORIES_URL", "https://bo.wix.com/bazel-repositories-server/repositories")
url_with_params = repositories_url + (
    "?list={repo_list}&branch={tracking_branch}".format(repo_list=repo_list, tracking_branch=tracking_branch))


def fetch_repositories(suppress_prints):
    logging.debug('second_party_resolved_dependencies env var = %s' % second_party_resolved_dependencies)
    if (second_party_resolved_dependencies is None) or (second_party_resolved_dependencies == second_party_resolved_dependencies_empty_placeholder):
        logging.debug("Fetching resolved dependencies from url:\t%s" % url_with_params)
        try:
            dependencies_raw_string = urlopen(url_with_params, timeout=5).read()
        except URLError as e:
            logging.debug("FAIL: fetching from BRS. URLError = %s", e)
            if not suppress_prints:
                print("Fetching from bazel repositories server failed (URLError)")
            sys.exit(1)
        except socket.timeout:
            if not suppress_prints:
                print("Fetching from bazel repositories server failed (Timeout)")
            sys.exit(1)
    else:
        logging.debug("Overriding resolved dependencies from env var SECOND_PARTY_RESOLVED_DEPENDENCIES")
        dependencies_raw_string = base64.b64decode(second_party_resolved_dependencies)
        if not suppress_prints:
            print("Using 2nd party dependencies from environment variable %s" % second_party_resolved_dependencies_env_var_name)
    io = StringIO()
    json.dump(json.loads(dependencies_raw_string)["repositories"], io, sort_keys=True, indent=4, separators=(',', ': '))
    json_file_repos = io.getvalue()
    starlark_file_repos = json.loads(dependencies_raw_string)["resolvedDependenciesFile"]
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
    encoded_out, err = process.communicate()
    out = encoded_out.decode("utf-8")
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


def write_repositories(workspace_dir, suppress_logs):
    current_branch = read_current_branch()
    starlark_file_path = "{}{}{}{}".format(workspace_dir, tools_relative_path, current_branch, starlark_file_name_postfix).replace(
        "\n", "")
    symlink_path = workspace_dir + symlink_relative_path
    if os.path.isfile(starlark_file_path) and file_is_not_empty(starlark_file_path):
        write_symlink_to_path(symlink_path, starlark_file_path)
        if not suppress_logs:
            print("Using 2nd party dependencies from a local versions file (%s)" % starlark_file_path)
        sys.exit(0)

    json_file_repos, starlark_file_repos = fetch_repositories(suppress_logs)

    json_file_path = "{}{}{}{}".format(workspace_dir, tools_relative_path, current_branch, json_file_name_postfix).replace("\n", "")
    write_content_to_path(json_file_repos, json_file_path)
    write_content_to_path(starlark_file_repos, starlark_file_path)
    write_symlink_to_path(symlink_path, starlark_file_path)

    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()
    if not suppress_logs:
        print("2nd party dependencies resolved!")


def parse_workspace_dir():
    parser = argparse.ArgumentParser()
    parser.add_argument('workspace_dir')
    parser.add_argument('suppress_logs')
    args = parser.parse_args()
    return args.workspace_dir, args.suppress_logs


def main():
    (workspace_dir, suppress_logs_string) = parse_workspace_dir()
    suppress_logs = suppress_logs_string == 'True'
    if not suppress_logs:
        print("Resolving 2nd party dependencies...")
    write_repositories(workspace_dir, suppress_logs)


if __name__ == "__main__":
    main()
