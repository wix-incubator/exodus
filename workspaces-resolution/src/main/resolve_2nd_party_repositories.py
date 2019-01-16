import argparse
import os.path
import base64

import zlib
import sys
sys.dont_write_bytecode = True
from utils import *


second_party_resolved_deps_override_empty_placeholder = "EMPTY!"
build_branch_override_empty_placeholder = "~EMPTY~"
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
    local_cache_starlark_file_path = create_local_cache_starlark_file_path(workspace_dir, read_current_branch())
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
            does_non_empty_file_exist(create_local_cache_starlark_file_path(workspace_dir, build_branch_override)):
        create_versions_files_from_other_branch(read_current_branch(), build_branch_override)
    else:
        create_versions_files_from_server(workspace_dir,
                                          repositories_url,
                                          repo_list,
                                          tracking_branch,
                                          should_suppress_prints,
                                          json_file_path_override)


def point_local_cache_symlink_to_path(starlark_file_path):
    write_symlink_to_path(local_cache_symlink_path(workspace_dir), starlark_file_path)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by using a local dependencies file)")


def point_local_cache_symlink_to_git_tracked_symlink():
    write_symlink_to_path(local_cache_symlink_path(workspace_dir), git_tracked_symlink_path(workspace_dir))
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by git tracked fixed dependencies)")


def can_use_existing_resolved_dependencies_from_local_cache(starlark_file_path):
    return does_non_empty_file_exist(starlark_file_path)


def can_use_existing_resolved_dependencies_from_git_tracked_folder():
    return does_non_empty_file_exist(git_tracked_symlink_path(workspace_dir))


def can_use_second_party_resolved_depts_override():
    return (second_party_resolved_deps_override is not None) and \
           (second_party_resolved_deps_override != second_party_resolved_deps_override_empty_placeholder)


def can_use_branch_override():
    return (build_branch_override is not None) and \
           (build_branch_override != build_branch_override_empty_placeholder) and \
           does_non_empty_file_exist(create_local_cache_starlark_file_path(workspace_dir, build_branch_override))


def create_version_files_from_deps_override(encoded_deps):
    logging.debug("Overriding resolved dependencies from env var SECOND_PARTY_RESOLVED_DEPENDENCIES")
    create_version_files_from_raw_string(base64.b64decode(encoded_deps), workspace_dir, json_file_path_override, should_suppress_prints)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by resolved dependencies override)")


def create_version_files_from_compressed_deps_override(encoded_deps):
    logging.debug("Overriding resolved dependencies from env var COMPRESSED_SECOND_PARTY_RESOLVED_DEPENDENCIES")
    decoded_deps = base64.b64decode(encoded_deps)
    uncompressed_deps = zlib.decompress(decoded_deps)
    create_version_files_from_raw_string(uncompressed_deps, workspace_dir,json_file_path_override, should_suppress_prints)
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by compressed resolved dependencies override)")


def create_versions_files_from_other_branch(current_branch, other_branch):
    logging.debug("Overriding build branch %s from env var BUILD_BRANCH_OVERRIDE" % other_branch)
    run_process(['cp', create_local_cache_starlark_file_path(workspace_dir, other_branch),
                 create_local_cache_starlark_file_path(workspace_dir, current_branch)],
                'Failed to create resolved dependencies file for branch %s' % current_branch)
    write_symlink_to_path(local_cache_symlink_path(workspace_dir), create_local_cache_starlark_file_path(workspace_dir, other_branch))
    if not should_suppress_prints:
        print("2nd party dependencies resolved! (by using build branch override)")


def load_environment_variables():
    global repo_list, tracking_branch, second_party_resolved_deps_override, compressed_second_party_resolved_deps_override, json_file_path_override, build_branch_override, repositories_url
    repo_list = os.environ.get("REPO_LIST", default_repo_list)
    logging.debug("[env var] repo_list = %s", repo_list)
    tracking_branch = os.environ.get("TRACKING_BRANCH", default_tracking_branch)
    logging.debug("[env var] tracking_branch = %s", tracking_branch)
    second_party_resolved_deps_override = os.environ.get(second_party_resolved_dependencies_env_var_name)
    logging.debug("[env var] second_party_resolved_deps_override = %s", second_party_resolved_deps_override)
    compressed_second_party_resolved_deps_override = os.environ.get(compressed_second_party_resolved_dependencies_env_var_name)
    logging.debug("[env var] compressed_second_party_resolved_deps_override = %s", compressed_second_party_resolved_deps_override)
    json_file_path_override = os.environ.get(json_file_path_override_env_var_name, json_file_path_override_empty_placeholder)
    logging.debug("[env var] json_file_path_override = %s", json_file_path_override)
    build_branch_override = os.environ.get("BUILD_BRANCH_OVERRIDE")
    logging.debug("[env var] build_branch_override = %s", build_branch_override)
    repositories_url = os.environ.get("REPOSITORIES_URL", default_repositories_url)
    logging.debug("[env var] repositories_url = %s", repositories_url)


if __name__ == "__main__":
    main()
