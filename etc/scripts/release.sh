#!/bin/sh
set -e

# The path to a directory where the code will be checked out and the assemblies would be generated. For example: /home/user/build. Required.
BUILD_DIRECTORY=$1
GIT_HUB_TOKEN=$2
GIT_HUB_READ_ONLY_TOKEN=$3
GIT_HUB_EMAIL=$4

# Whether the source code and assemblies on the build directory should be cleaned or not
CLEAN=false

REPOSITORY_OWNER=maxirosson
PROJECT_NAME=jdroid-java-github

PROJECT_DIRECTORY=$BUILD_DIRECTORY/$PROJECT_NAME
SOURCE_DIRECTORY=$PROJECT_DIRECTORY/sources
ASSEMBLIES_DIRECTORY=$PROJECT_DIRECTORY/assemblies
PROJECT_HOME=$SOURCE_DIRECTORY/$PROJECT_NAME

# ************************
# Parameters validation
# ************************

if [ -z "$BUILD_DIRECTORY" ]
then
	echo "[ERROR] The BUILD_DIRECTORY parameter is required"
	exit 1;
fi

if [ ! -d "$BUILD_DIRECTORY" ]
then
	echo "[ERROR] - The BUILD_DIRECTORY directory does not exist."
	exit 1;
fi

if [ -z "$GIT_HUB_TOKEN" ]
then
	echo "[ERROR] The GIT_HUB_TOKEN parameter is required"
	exit 1;
fi

# ************************
# Checking out
# ************************

if [ "$CLEAN" = "true" ] || [ ! -d "$SOURCE_DIRECTORY" ]
then
	# Clean the directories
	rm -r -f $SOURCE_DIRECTORY
	mkdir -p $SOURCE_DIRECTORY

	# Checkout the project
	cd $SOURCE_DIRECTORY
	echo Cloning git@github.com:$REPOSITORY_OWNER/$PROJECT_NAME.git
	git clone git@github.com:$REPOSITORY_OWNER/$PROJECT_NAME.git $PROJECT_NAME
fi
cd $PROJECT_HOME
git config user.email $GIT_HUB_EMAIL

# ************************
# Synch production branch
# ************************

git add -A
git stash
git checkout production
git pull

VERSION=`./gradlew :printVersion -q --configure-on-demand -PSNAPSHOT=false`

# ************************
# Close Milestone & Upload Release on GitHub
# ************************

./gradlew :closeGitHubMilestone :createGitHubRelease --configure-on-demand -PSNAPSHOT=false -PREPOSITORY_OWNER=$REPOSITORY_OWNER -PREPOSITORY_NAME=$PROJECT_NAME -PGITHUB_OATH_TOKEN=$GIT_HUB_TOKEN

# ************************
# Generate Change Log
# ************************

github_changelog_generator --no-unreleased --no-pull-requests --no-pr-wo-labels --exclude-labels task -t $GIT_HUB_READ_ONLY_TOKEN

git add CHANGELOG.md
git commit -m "Updated CHANGELOG.md"
git diff HEAD

read -p "Please verify the $PROJECT_HOME/CHANGELOG.md and press [Enter] key to continue..."

git push origin HEAD:production

# ************************
# Deploy to Sonatype repository
# ************************

cd $PROJECT_HOME

cmd="./gradlew clean uploadArchives -PSNAPSHOT=false -PLOCAL_UPLOAD=false"

echo "Executing the following command"
echo "${cmd}"
eval "${cmd}"
