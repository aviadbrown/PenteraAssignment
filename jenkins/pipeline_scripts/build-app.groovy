#!/usr/bin/env groovy

import java.util.logging.Logger



// Define Initial Variables
Logger logger = Logger.getLogger('org.example.jobdsl')
String slaveName = "master" // TODO: Add option to run on other slave dynamically. Now hardcoded to master as there is no slave configured


properties([
        parameters([
                choice(name: 'selectedVersionType', choices: ['', 'patch', 'minor', 'major'], description: 'Choose a bump type for the version'),
                booleanParam(name: 'publishVersion', defaultValue: false, description: 'Publish the version - push the Docker image to a registry and push the changes to the repo'),
                booleanParam(name: 'loadParamsOnly', defaultValue: false, description: 'Load parameters only')
        ])
])


if (env.BUILD_NUMBER.equals('1') || params.loadParamsOnly.toBoolean()) {
    logger.info("Loading parameters only")
    currentBuild.displayName = "#${env.BUILD_NUMBER}-parameters-loading"
    return
}

currentBuild.displayName = "#${env.BUILD_NUMBER}-${params.selectedVersionType}-bump"

ansiColor('xterm') {
    timeout(time: 15, unit: 'MINUTES') {
        timestamps {
            node(slaveName) {
                try {
                    logger.info("Starting - selectedVersionType: ${params.selectedVersionType}, publishVersion: ${params.publishVersion}")
                    stage('Checkout app repo') {
                        checkout([$class: 'GitSCM',
                                  branches: [[name: "*/main"]], // TODO: Add option to choose another branch
                                  doGenerateSubmoduleConfigurations: false,
                                  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "."]],
                                  submoduleCfg: [],
                                  userRemoteConfigs: [[credentialsId: 'jenkins-github', url: 'git@github.com:aviadbrown/PenteraAssignment.git']]
                        ])
                    }
                    dir('app') {
                        stage('Bump app version') {
                            String prevVersion = getPoetryVersion()
                            logger.info("----------> Prev app version: ${prevVersion}")

                            if (params.selectedVersionType.equals('')) {
                                logger.info("No version bump selected, skipping the bumping process")
                            } else {
                                // Bump the version based on the selected bump type
                                bumpPoetryVersion(params.selectedVersionType)
                                newVersion = getPoetryVersion()
                                currentBuild.displayName = "#${env.BUILD_NUMBER}-${params.selectedVersionType}-bump-to-${newVersion}"
                                logger.info("----------> New app version: ${currVersion}")
                            }
                        }

                        // Set the Docker image tag based on the bumped version
                        String dockerImageTag = "devops-web-app:${newVersion}"
                        logger.info("----------> The new Docker image tag: ${dockerImageTag}")

                        stage('Build Docker image') {
                            buildDockerImage(dockerImageTag)
                        }

                        stage('Push Docker image to registry') {
                            if (params.publishVersion.toBoolean()) {
                                pushDockerImage(dockerImageTag)
                            } else {
                                logger.info("Skipping the push to the registry")
                            }
                        }

                        stage('Push changes to the repo') {
                            if (params.publishVersion.toBoolean()) {
                                pushToGithub()
                            } else {
                                logger.info("Skipping the push to the repo")
                            }
                        }
                    }
                } catch (Exception e) {
                    currentBuild.result = 'FAILED'
                    errMSg = "ERROR: An error occurred in stage '${env.STAGE_NAME}': ${e}"
                    logger.severe("------> ${errMsg}")
                    throw new Exception(errMsg)
                } finally {
                    // Clean up
                    cleanWs()
                }
            }
        }
    }
}

def bumpPoetryVersion(bumpType) {
    logger.debug("Bumping the version with type: ${bumpType}")
    sh "poetry version ${bumpType}"
}

String getPoetryVersion() {
    String version = sh(script: "poetry version -s", returnStdout: true).trim()
    logger.debug("Poetry version: ${version}")
    return version
}

def buildDockerImage(String dockerImageTag) {
    logger.info("Building the Docker image with tag: ${dockerImageTag}")
    // Build the Docker image using the specified tag
    sh "docker build -t ${dockerImageTag} ."
    logger.info("Docker image built successfully")
}

def pushDockerImage(String dockerImageTag) {
    logger.info("Pushing the Docker image with tag: ${dockerImageTag}")
    // Push the Docker image to a registry
    sh "docker push ${dockerImageTag}"
    logger.info("Docker image pushed successfully")
}

def pushToGithub() {
    sh """
        git add .
        sh "git commit -m 'Bump version to ${newVersion}'"
        sh "git push origin main"
    """
}