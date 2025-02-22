#!/bin/bash

# Ensure that environment variables are loaded
if [ -f .env ]; then
    # Load environment variables from the .env file
    source .env
else
    echo "Error: .env file not found. Please ensure it exists and contains your GITHUB_USERNAME and GITHUB_TOKEN."
    exit 1
fi

# Check if a version number is provided
if [ -z "$1" ]; then
    echo "Error: Please provide a version number (e.g., 1.1.0, 1.0.1)."
    exit 1
fi

NEW_VERSION=$1

# Print the current version
echo "Current version in build.gradle:"
grep "version = " build.gradle

# Update the version in build.gradle
echo "Updating version in build.gradle to $NEW_VERSION..."
sed -i "s/version = '[^']*'/version = '$NEW_VERSION'/g" build.gradle

# Build the project
echo "Building the project with Gradle..."
./gradlew clean build

# Check if the build was successful
if [ $? -ne 0 ]; then
    echo "Error: Build failed. Please fix any issues and try again."
    exit 1
fi

# Publish the new version to GitHub Packages
echo "Publishing version $NEW_VERSION to GitHub Packages..."
./gradlew publish

# Check if the publish was successful
if [ $? -eq 0 ]; then
    echo "SDK version $NEW_VERSION successfully uploaded to GitHub Packages!"
else
    echo "Error: Publishing failed. Please check for issues and try again."
    exit 1
fi

# Confirm the version update
echo "Version $NEW_VERSION has been successfully set and uploaded."