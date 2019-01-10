import logging
import os
import subprocess

logging_level = logging.DEBUG if "DEBUG_2ND_PARTY_SCRIPT" in os.environ else logging.INFO
logging.basicConfig(level=logging_level, format='%(asctime)s  %(levelname)s: %(message)s')


def read_current_branch(directory):
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
