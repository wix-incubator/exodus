import sys
import argparse
import os
import logging
sys.dont_write_bytecode = True

from python_utils.output_to_shell import OutputToShell, print_message
from python_utils.logger import set_logging_level

set_logging_level()


def main():
    (workspace_dir, should_suppress_prints) = parse_arguments()
    OutputToShell.should_suppress_prints = should_suppress_prints
    print_message("Resolving 2nd party dependencies")
    build_type = get_build_type_from_env_var()
    resolve_according_to_build_type(build_type, workspace_dir)
    create_build_files_if_needed(workspace_dir)


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('workspace_dir')
    parser.add_argument('suppress_prints')
    args = parser.parse_args()
    workspace_dir = args.workspace_dir
    should_suppress_prints = args.suppress_prints == 'True'
    logging.debug("workspace_dir = {}\nshould_suppress_prints = {}".format(workspace_dir, should_suppress_prints))
    return workspace_dir, should_suppress_prints


def get_build_type_from_env_var():
    build_type = os.environ.get("BUILD_TYPE", "")
    logging.debug("[env var] BUILD_TYPE = %s", build_type)
    return build_type


def resolve_according_to_build_type(build_type, workspace_dir):
    if build_type == "branch_only":
        from resolving_lib.resolving_for_ci_branch_build import resolve_2nd_party_repositories
    elif build_type == "build_master":
        from resolving_lib.resolving_for_ci_master_build import resolve_2nd_party_repositories
    elif build_type == "merge_dry_run":
        from resolving_lib.resolving_for_ci_pr_build import resolve_2nd_party_repositories
    else:
        from resolving_lib.resolving_for_local_build import resolve_2nd_party_repositories
    resolve_2nd_party_repositories(workspace_dir)


def create_build_files_if_needed(workspace_dir):
    logging.debug("Generating %s" % workspace_dir + "/BUILD.bazel")
    open(workspace_dir + "/BUILD.bazel", 'a').close()
    logging.debug("Generating %s" % workspace_dir + "/tools/BUILD.bazel")
    open(workspace_dir + "/tools/BUILD.bazel", 'a').close()


if __name__ == "__main__":
    main()
