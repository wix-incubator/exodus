import argparse
import json
import os
import os.path
import subprocess
import sys
import logging
import base64
import socket
import zlib

try:
    # python 2
    from StringIO import StringIO
except ImportError:
    # python 3
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
versions_files_local_cache_folder = tools_relative_path + "2nd_party_resolved_dependencies/"
starlark_file_name_postfix = "_2nd_party_resolved_dependencies.bzl"
default_json_file_name = "2nd_party_resolved_dependencies.json"
local_cache_symlink_relative_path = tools_relative_path + "2nd_party_resolved_dependencies_current_branch.bzl"
git_tracked_symlink_relative_path = tools_relative_path + "fixed_2nd_party_resolved_dependencies.bzl"
second_party_resolved_deps_override_empty_placeholder = "EMPTY!"
build_branch_override_empty_placeholder = "~EMPTY~"
json_file_path_override_empty_placeholder = "EMPTY"
second_party_resolved_dependencies_env_var_name = "SECOND_PARTY_RESOLVED_DEPENDENCIES"
compressed_second_party_resolved_dependencies_env_var_name = "COMPRESSED_SECOND_PARTY_RESOLVED_DEPENDENCIES"
json_file_path_override_env_var_name = "RESOLVED_2ND_DEPENDENCIES_JSON_PATH"


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
    local_cache_starlark_file_path = create_local_cache_starlark_file_path(read_current_branch())
    if can_use_existing_resolved_dependencies_from_local_cache(local_cache_starlark_file_path):
        point_local_cache_symlink_to_path(local_cache_starlark_file_path)
    elif (compressed_second_party_resolved_deps_override is not None) and \
            (compressed_second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder):
        create_version_files_from_compressed_deps_override(compressed_second_party_resolved_deps_override)
    elif (second_party_resolved_deps_override is not None) and \
            (second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder):
        create_version_files_from_deps_override(second_party_resolved_deps_override)
    elif can_use_existing_resolved_dependencies_from_git_tracked_folder():
        point_local_cache_symlink_to_git_tracked_symlink()
    elif (build_branch_override is not None) and \
            (build_branch_override != build_branch_override_empty_placeholder) and \
            does_non_empty_file_exist(create_local_cache_starlark_file_path(build_branch_override)):
        create_versions_files_from_other_branch(read_current_branch(), build_branch_override)
    else:
        create_versions_files_from_server()


def point_local_cache_symlink_to_path(starlark_file_path):
    write_symlink_to_path(local_cache_symlink_path(), starlark_file_path)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by using a local dependencies file)")


def point_local_cache_symlink_to_git_tracked_symlink():
    write_symlink_to_path(local_cache_symlink_path(), git_tracked_symlink_path())
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by git tracked fixed dependencies)")


def can_use_existing_resolved_dependencies_from_local_cache(starlark_file_path):
    return does_non_empty_file_exist(starlark_file_path)


def can_use_existing_resolved_dependencies_from_git_tracked_folder():
    return does_non_empty_file_exist(git_tracked_symlink_path())


def can_use_second_party_resolved_depts_override():
    return (second_party_resolved_deps_override is not None) and \
           (second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder)


def can_use_branch_override():
    return (build_branch_override is not None) and \
           (build_branch_override != build_branch_override_empty_placeholder) and \
           does_non_empty_file_exist(create_local_cache_starlark_file_path(build_branch_override))


def create_version_files_from_deps_override(encoded_deps):
    logging.debug("Overriding resolved dependencies from env var SECOND_PARTY_RESOLVED_DEPENDENCIES")
    create_version_files_from_raw_string(base64.b64decode(encoded_deps))
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by resolved dependencies override)")


def create_version_files_from_compressed_deps_override(encoded_deps):
    logging.debug("Overriding resolved dependencies from env var COMPRESSED_SECOND_PARTY_RESOLVED_DEPENDENCIES")
    decoded_deps = base64.b64decode(encoded_deps)
    uncompressed_deps = zlib.decompress(decoded_deps)
    create_version_files_from_raw_string(uncompressed_deps)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by compressed resolved dependencies override)")


def create_versions_files_from_other_branch(current_branch, other_branch):
    logging.debug("Overriding build branch %s from env var BUILD_BRANCH_OVERRIDE" % other_branch)
    run_process(['cp', create_local_cache_starlark_file_path(other_branch),
                 create_local_cache_starlark_file_path(current_branch)],
                'Failed to create resolved dependencies file for branch %s' % current_branch)
    write_symlink_to_path(local_cache_symlink_path(), create_local_cache_starlark_file_path(other_branch))
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by using build branch override)")


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
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by fetching from bazel repositories server)")


def create_version_files_from_raw_string(dependencies_raw_string):
    json_file_repos, starlark_file_repos = decode_dependencies_to_files(dependencies_raw_string)
    create_version_files(read_current_branch(), json_file_repos, starlark_file_repos)


def create_version_folder_if_not_exists():
    versions_folder_full_path = "{}{}".format(workspace_dir, versions_files_local_cache_folder)
    if not os.path.exists(versions_folder_full_path):
        logging.debug("Creating folder: {}".format(versions_folder_full_path))
        os.makedirs(versions_folder_full_path)


def create_version_files(current_branch, json_file_repos, starlark_file_repos):
    starlark_file_path = create_local_cache_starlark_file_path(current_branch)
    json_file_path = get_json_file_path()

    create_version_folder_if_not_exists()
    write_content_to_path(json_file_repos, json_file_path)
    write_content_to_path(starlark_file_repos, starlark_file_path)
    write_symlink_to_path(local_cache_symlink_path(), starlark_file_path)

    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()


def get_json_file_path():
    if json_file_path_override is not None and json_file_path_override != json_file_path_override_empty_placeholder:
        path = "{}/{}".format(workspace_dir, json_file_path_override)
        logging.debug("Versions used in this build will be written to {}".format(path))
        if not should_suppress_prints:
            print("Versions used in this build will be written to {}".format(path))
        return path
    return "{}{}{}".format(workspace_dir, versions_files_local_cache_folder, default_json_file_name)


def does_non_empty_file_exist(path):
    return os.path.isfile(path) and os.stat(path).st_size != 0


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
    with open(path, "w+") as opened_file:
        logging.debug("Generating %s with content:\n%s" % (path, content))
        opened_file.write(content)
        opened_file.close()


def load_environment_variables():
    global repo_list, tracking_branch, second_party_resolved_deps_override, compressed_second_party_resolved_deps_override, json_file_path_override, build_branch_override, repositories_url
    repo_list = os.environ.get("REPO_LIST", "default")
    logging.debug("[env var] repo_list = %s", repo_list)
    tracking_branch = os.environ.get("TRACKING_BRANCH", "master")
    logging.debug("[env var] tracking_branch = %s", tracking_branch)
    second_party_resolved_deps_override = os.environ.get(second_party_resolved_dependencies_env_var_name)
    logging.debug("[env var] second_party_resolved_deps_override = %s", second_party_resolved_deps_override)
    compressed_second_party_resolved_deps_override = os.environ.get(compressed_second_party_resolved_dependencies_env_var_name)
    logging.debug("[env var] compressed_second_party_resolved_deps_override = %s", compressed_second_party_resolved_deps_override)
    json_file_path_override = os.environ.get(json_file_path_override_env_var_name)
    logging.debug("[env var] json_file_path_override = %s", json_file_path_override)
    build_branch_override = os.environ.get("BUILD_BRANCH_OVERRIDE")
    logging.debug("[env var] build_branch_override = %s", build_branch_override)
    repositories_url = os.environ.get("REPOSITORIES_URL", "https://bo.wix.com/bazel-repositories-server/repositories")
    logging.debug("[env var] repositories_url = %s", repositories_url)


def create_local_cache_starlark_file_path(branch):
    escaped_branch = branch.replace("/", "..")
    return "{}{}{}{}".format(workspace_dir, versions_files_local_cache_folder, escaped_branch, starlark_file_name_postfix).replace(
        "\n", "")


def local_cache_symlink_path():
    return workspace_dir + local_cache_symlink_relative_path


def git_tracked_symlink_path():
    return workspace_dir + git_tracked_symlink_relative_path


def decode_dependencies_to_files(dependencies_raw_string):
    io = StringIO()
    json.dump(json.loads(dependencies_raw_string)["repositories"], io, sort_keys=True, indent=4, separators=(',', ': '))
    json_file_repos = io.getvalue()
    starlark_file_repos = json.loads(dependencies_raw_string)["resolvedDependenciesFile"]
    return json_file_repos, starlark_file_repos


if __name__ == "__main__":
    main()
