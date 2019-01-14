import logging
import os
import subprocess

logging_level = logging.DEBUG if "DEBUG_2ND_PARTY_SCRIPT" in os.environ else logging.INFO
logging.basicConfig(level=logging_level, format='%(asctime)s  %(levelname)s: %(message)s')

git_tracked_symlink_file_name = "fixed_resolved_dependencies_for_ci_branch_build.bzl"


def read_current_branch(directory=None):
    fail_message = "Failed to read the current branch"
    if directory is None:
        branch = run_process(['git', 'rev-parse', '--abbrev-ref', 'HEAD'], fail_message)
    else:
        branch = run_process(['git', '-C', directory, 'rev-parse', '--abbrev-ref', 'HEAD'], fail_message)
    return branch.replace("\n", "")


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


def write_symlink_to_path(link_path, path):
    run_process(['ln', '-sf', path, link_path], "Failed to write symlink %s => %s" % (link_path, path))

