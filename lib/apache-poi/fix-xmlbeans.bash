#!/bin/bash
#
# This script removes the `org/w3c` classes from the `xmlbeans-2.3.0.jar` jar file
# and then creates a new jar file `xmlbeans-2.3.0-fixed.jar`.
# The resulting jar file should be used in Eclipse library dependency and the installer build.

tmpFolder="/tmp/fix-xmlbeans"
jarExe="/c/Program Files/java/jdk11/bin/jar"

# Script folder.
scriptFolder=$(cd $(dirname "$0") && pwd)
jarFile="${scriptFolder}/xmlbeans-2.3.0.jar"
newJarFile="${scriptFolder}/xmlbeans-2.3.0-fixed.jar"

# Make sure that the program exists.
if [ ! -e "${jarExe}" ]; then
  echo "jar program executable does not exist:  ${jarExe}"
  exit 1
fi

# Remove the temporary folder.

rm -rf "${tmpFolder}"

# Recreate the temporary folder.

echo "Creating temporary folder: ${tmpFolder}"
mkdir ${tmpFolder}

# Use jar to uncompress the files.

echo "Extracting file: ${jarFile}"
cd ${tmpFolder}
"${jarExe}" xf ${jarFile}

# Remove the class files that are not needed.
echo "Removing org/w3c folder from extracted files."
rm -rf "${tmpFolder}/org/w3c"

# TODO - could also remove the files from the index
# /tmp/fix-xmlbeans/META-INF/INDEX.LIST

# Recreate the Jar file.
echo "Creating file: ${jarFile}"
"${jarExe}" cf "${newJarFile}" *

# Change back to the original folder:
cd "${scriptFolder}"

# Check to make sure files are not in the file.
echo "Checking for org/w3c files in new jar file (should be none listed)."
"${jarExe}" tf "${newJarFile}" | grep "/org/w3c"

exit 0
