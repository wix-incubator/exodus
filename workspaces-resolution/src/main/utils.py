import logging
import os
import subprocess
import sys
import socket
import json

sys.dont_write_bytecode = True

if sys.version_info[0] == 3:
    from urllib.request import urlopen
    from urllib.error import URLError
else:
    # Not Python 3 - today, it is most likely to be Python 2
    # But note that this might need an update when Python 4
    # might be around one day
    from urllib2 import urlopen
    from urllib2 import URLError

try:
    # python 2
    from StringIO import StringIO
except ImportError:
    # python 3
    from io import StringIOfin

logging_level = logging.DEBUG if "DEBUG_2ND_PARTY_SCRIPT" in os.environ else logging.INFO
logging.basicConfig(level=logging_level, format='%(asctime)s  %(levelname)s: %(message)s')

local_cache_versions_folder = "2nd_party_resolved_dependencies"
git_tracked_versions_folder = "fixed_2nd_party_resolved_dependencies"
default_repositories_url = "https://bo.wix.com/bazel-repositories-server/repositories"
tools_relative_path = "/tools/"
versions_files_local_cache_folder = tools_relative_path + local_cache_versions_folder + "/"
starlark_file_name_postfix = "_2nd_party_resolved_dependencies.bzl"
local_cache_symlink_relative_path = tools_relative_path + "2nd_party_resolved_dependencies_current_branch.bzl"
git_tracked_symlink_file_name = "fixed_resolved_dependencies_for_ci_branch_build.bzl"
json_file_path_override_empty_placeholder = "EMPTY"
default_json_file_name = "2nd_party_resolved_dependencies.json"

default_repo_list = "default"
default_tracking_branch = "master"

folder_of_script = os.path.dirname(sys.argv[0])


def read_current_branch(directory=None):
    fail_message = "Failed to read the current branch"
    if directory is None:
        branch = run_process(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], fail_message)
    else:
        branch = run_process(['git', '-C', directory, 'rev-parse', '--abbrev-ref', 'HEAD'], fail_message)
    return branch.replace("\n", "")


def run_process(splitted_command, fail_msg):
    logging.debug("Running:\t{}".format(' '.join(splitted_command)))
    process = subprocess.Popen(splitted_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=folder_of_script)
    encoded_out, err = process.communicate()
    out = encoded_out.decode("utf-8")
    if err:
        msg = "%s. stderr = %s" % (fail_msg, err)
        logging.error(msg)
        raise Exception(msg)
    logging.debug("Output:\t%s" % out)
    return out


def write_symlink_to_path(link_path, path):
    run_process(['ln', '-sf', path, link_path], "Failed to write symlink %s => %s" % (link_path, path))


def does_non_empty_file_exist(path):
    return os.path.isfile(path) and os.stat(path).st_size != 0


def git_tracked_symlink_path(workspace_dir):
    return workspace_dir + tools_relative_path + git_tracked_symlink_file_name


def create_versions_files_from_server(workspace_dir,
                                      repositories_url=default_repositories_url,
                                      repo_list=default_repo_list,
                                      tracking_branch=default_tracking_branch,
                                      should_suppress_prints=True,
                                      json_file_path_override=json_file_path_override_empty_placeholder):
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
    create_version_files_from_raw_string(dependencies_raw_string,
                                         workspace_dir,
                                         json_file_path_override,
                                         should_suppress_prints)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by fetching from bazel repositories server)")


def create_version_files_from_raw_string(dependencies_raw_string,
                                         workspace_dir,
                                         json_file_path_override,
                                         should_suppress_prints):
    json_file_repos, starlark_file_repos = decode_dependencies_to_files(dependencies_raw_string)
    create_version_files(workspace_dir, read_current_branch(workspace_dir), json_file_repos, starlark_file_repos,
                         json_file_path_override, should_suppress_prints)


def create_version_files(workspace_dir,
                         current_branch,
                         json_file_repos,
                         starlark_file_repos,
                         json_file_path_override,
                         should_suppress_prints):
    create_starlark_versions_file(workspace_dir, current_branch, starlark_file_repos)
    create_json_versions_file(workspace_dir, json_file_repos, json_file_path_override, should_suppress_prints)

    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()


def create_starlark_versions_file(workspace_dir, current_branch, starlark_file_repos):
    starlark_file_path = local_cache_starlark_file_path(workspace_dir, current_branch)
    create_version_folder_if_not_exists(workspace_dir)
    write_content_to_path(starlark_file_repos, starlark_file_path)
    write_symlink_to_path(local_cache_symlink_path(workspace_dir), starlark_file_path)


def create_json_versions_file(workspace_dir, json_file_repos, json_file_path_override, should_suppress_prints):
    json_file_path = get_json_file_path(workspace_dir, json_file_path_override, should_suppress_prints)
    create_version_folder_if_not_exists(workspace_dir)
    write_content_to_path(json_file_repos, json_file_path)


def decode_dependencies_to_files(dependencies_raw_string):
    io = StringIO()
    json.dump(json.loads(dependencies_raw_string)["repositories"], io, sort_keys=True, indent=4, separators=(',', ': '))
    json_file_repos = io.getvalue()
    starlark_file_repos = json.loads(dependencies_raw_string)["resolvedDependenciesFile"]
    return json_file_repos, starlark_file_repos


def local_cache_starlark_file_path(workspace_dir, branch):
    escaped_branch = branch.replace("/", "..")
    return "{}{}{}{}".format(workspace_dir, versions_files_local_cache_folder, escaped_branch,
                             starlark_file_name_postfix).replace("\n", "")


def create_version_folder_if_not_exists(workspace_dir):
    versions_folder_full_path = "{}{}".format(workspace_dir, versions_files_local_cache_folder)
    if not os.path.exists(versions_folder_full_path):
        logging.debug("Creating versions folder: {}".format(versions_folder_full_path))
        os.makedirs(versions_folder_full_path)


def write_content_to_path(content, path):
    with open(path, "w+") as opened_file:
        logging.debug("Generating %s with content:\n%s" % (path, content))
        opened_file.write(content)
        opened_file.close()


def local_cache_symlink_path(workspace_dir):
    return workspace_dir + local_cache_symlink_relative_path


def get_json_file_path(workspace_dir, json_file_path_override, should_suppress_prints):
    if json_file_path_override != json_file_path_override_empty_placeholder:
        path = "{}/{}".format(workspace_dir, json_file_path_override)
        logging.debug("Versions used in this build will be written to {}".format(path))
        if not should_suppress_prints:
            print("Versions used in this build will be written to {}".format(path))
        return path
    return "{}{}{}".format(workspace_dir, versions_files_local_cache_folder, default_json_file_name)

