#!/usr/bin/env groovy

// Name of the Jenkins slave to run the pipeline on, now hardcoded to master as I don't have any slaves configured
slaveName = "master"
properties([
    parameters([
            choice(name: 'selectedVersionType', choices: ['patch', 'minor', 'major'], description: 'Choose a bump type for the version'),
            booleanParam(name: 'pushToRegistry', defaultValue: false, description: 'Push the Docker image to a registry after building'),

            booleanParam(name: 'loadParamsOnly', defaultValue: false, description: 'Load parameters only')
    ])
])

node('master') {
    // Bump the version based on the selected bump type
    currVersion = getPoetryVersion()
    bumpPoetryVersion(selectedVersionType)
    newVersion = getPoetryVersion()

    // Set the Docker image tag based on the bumped version
    String dockerImageTag = "devops-web-app:${newVersion}"

    // Build the Docker image using the specified tag
    sh "docker build -t ${dockerImageTag} -f Dockerfile ."

    // Optionally, push the image to a registry (e.g., Docker Hub)
    sh "docker push ${dockerImageTag}"
}

def bumpPoetryVersion(bumpType) {
    sh "poetry version ${bumpType}"
}

String getPoetryVersion() {
    String version = sh(script: "poetry version -s", returnStdout: true).trim()
    return version
}