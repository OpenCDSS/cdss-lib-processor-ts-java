#!/bin/bash
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required
# The above line ensures that the script can be run on Cygwin/Linux even with Windows CRNL

# Check command line arguments for the help command, or if 2 commands were not given
if [ $# -eq 1 ] && [ $1 = "--help" ]; then
	echo "Help"
	exit 0
elif [ $# -ne 2 ]; then
	echo "  Usage: ./create-mailpass.sh <Email_Account_ID> <Email_Account_Password>"
	echo "  or ./create-mailpass.sh --help for more information"
	exit 1
else
	accountID=$1
	accountPassword=$2
fi

#
mailPassFile=$HOME/.mailpass
if [ ! -f ${mailPassFile} ]; then
	touch ${mailPassFile}
	chmod 600 ${mailPassFile}
fi

# Write the user ID and password to the file. Overwrite each time.
echo ${accountID}:${accountPassword} > ${mailPassFile}
