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
second_party_resolved_deps_override_empty_placeholder = "EMPTY!"
build_branch_override_empty_placeholder = "~EMPTY~"
second_party_resolved_dependencies_env_var_name = "SECOND_PARTY_RESOLVED_DEPENDENCIES"


def main():
    load_environment_variables()
    parse_arguments()
    if not should_suppress_prints:
        print("Resolving 2nd party dependencies")
    resolve_repositories()


def parse_arguments():
    global workspace_dir, should_suppress_prints
    parser = argparse.ArgumentParser()
    parser.add_argument('workspace_dir')
    parser.add_argument('suppress_prints')
    args = parser.parse_args()
    workspace_dir = args.workspace_dir
    should_suppress_prints = args.suppress_prints == 'True'


def resolve_repositories():
    starlark_file_path = create_starlark_file_path(read_current_branch())
    if can_use_existing_resolved_dependencies(starlark_file_path):
        write_symlink_to_path(symlink_path(), starlark_file_path)
        if not should_suppress_prints:
            print("2nd party dependencies resolved! (by using a local dependencies file)")
    elif (second_party_resolved_deps_override is not None) and \
            (second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder):
        create_version_files_from_deps_override(second_party_resolved_deps_override)
        if not should_suppress_prints:
            print("2nd party dependencies resolved! (by resolved dependencies override)")
    elif (build_branch_override is not None) and \
            (build_branch_override != build_branch_override_empty_placeholder) and \
            does_non_empty_file_exist(create_starlark_file_path(build_branch_override)):
        create_versions_files_from_other_branch(read_current_branch(), build_branch_override)
        if not should_suppress_prints:
            print("2nd party dependencies resolved! (by using build branch override)")
    else:
        create_versions_files_from_server()
        if not should_suppress_prints:
            print("2nd party dependencies resolved! (by fetching from bazel repositories server)")


def can_use_existing_resolved_dependencies(starlark_file_path):
    return does_non_empty_file_exist(starlark_file_path)


def can_use_second_party_resolved_depts_override():
    return (second_party_resolved_deps_override is not None) and \
           (second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder)


def can_use_branch_override():
    return (build_branch_override is not None) and \
           (build_branch_override != build_branch_override_empty_placeholder) and \
           does_non_empty_file_exist(create_starlark_file_path(build_branch_override))


def create_version_files_from_deps_override(encoded_deps):
    logging.debug("Overriding resolved dependencies from env var SECOND_PARTY_RESOLVED_DEPENDENCIES")
    create_version_files_from_raw_string(base64.b64decode(encoded_deps))


def create_versions_files_from_other_branch(current_branch, other_branch):
    logging.debug("Overriding build branch %s from env var BUILD_BRANCH_OVERRIDE" % other_branch)
    run_process(['cp', create_starlark_file_path(other_branch),
                 create_starlark_file_path(current_branch)],
                'Failed to create resolved dependencies file for branch %s' % current_branch)
    write_symlink_to_path(symlink_path(), create_starlark_file_path(other_branch))


def create_versions_files_from_server():
    url_with_params = repositories_url + (
        "?list={repo_list}&branch={tracking_branch}".format(repo_list=repo_list, tracking_branch=tracking_branch))
    logging.debug("Fetching resolved dependencies from url:\t%s" % url_with_params)
    try:
        dependencies_raw_string = urlopen(url_with_params, timeout=5).read()
    except URLError as e:
        logging.debug("FAIL: fetching from BRS. URLError = %s", e)
        if not should_suppress_prints:
            print("Fetching from bazel repositories server failed (URLError)")
        sys.exit(1)
    except socket.timeout:
        if not should_suppress_prints:
            print("Fetching from bazel repositories server failed (Timeout)")
        sys.exit(1)
    create_version_files_from_raw_string(dependencies_raw_string)


def create_version_files_from_raw_string(dependencies_raw_string):
    json_file_repos, starlark_file_repos = decode_dependencies_to_files(dependencies_raw_string)
    create_version_files(read_current_branch(), json_file_repos, starlark_file_repos)


def create_version_files(current_branch, json_file_repos, starlark_file_repos):
    json_file_path = "{}{}{}{}".format(workspace_dir, tools_relative_path, current_branch, json_file_name_postfix).replace("\n", "")
    starlark_file_path = create_starlark_file_path(current_branch)

    write_content_to_path(json_file_repos, json_file_path)
    write_content_to_path(starlark_file_repos, starlark_file_path)
    write_symlink_to_path(symlink_path(), starlark_file_path)

    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()


def does_non_empty_file_exist(path):
    return os.path.isfile(path) and os.stat(path).st_size != 0


def read_current_repo_url():
    return run_process(['git', 'config', '--get', 'remote.origin.url'],
                       "Failed to read the current repository remote origin url")


def read_current_branch():
    return run_process(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], "Failed to read the current branch")


def write_symlink_to_path(link_path, path):
    run_process(['ln', '-sf', path, link_path], "Failed to write symlink %s => %s" % (link_path, path))


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


def load_environment_variables():
    global repo_list, tracking_branch, second_party_resolved_deps_override, build_branch_override, repositories_url
    repo_list = os.environ.get("REPO_LIST", "default")
    logging.debug("[env var] repo_list = %s", repo_list)
    tracking_branch = os.environ.get("TRACKING_BRANCH", "master")
    logging.debug("[env var] tracking_branch = %s", tracking_branch)
    second_party_resolved_deps_override = os.environ.get(second_party_resolved_dependencies_env_var_name)
    logging.debug("[env var] second_party_resolved_deps_override = %s", second_party_resolved_deps_override)
    build_branch_override = os.environ.get("BUILD_BRANCH_OVERRIDE")
    logging.debug("[env var] build_branch_override = %s", build_branch_override)
    repositories_url = os.environ.get("REPOSITORIES_URL", "https://bo.wix.com/bazel-repositories-server/repositories")
    logging.debug("[env var] repositories_url = %s", repositories_url)


def create_starlark_file_path(branch):
    return "{}{}{}{}".format(workspace_dir, tools_relative_path, branch, starlark_file_name_postfix).replace(
        "\n", "")


def symlink_path():
    return workspace_dir + symlink_relative_path


def decode_dependencies_to_files(dependencies_raw_string):
    io = StringIO()
    json.dump(json.loads(dependencies_raw_string)["repositories"], io, sort_keys=True, indent=4, separators=(',', ': '))
    json_file_repos = io.getvalue()
    starlark_file_repos = json.loads(dependencies_raw_string)["resolvedDependenciesFile"]
    return json_file_repos, starlark_file_repos


if __name__ == "__main__":
    main()
