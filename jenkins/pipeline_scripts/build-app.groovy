#!/usr/bin/env groovy

// Create a logger class to print colored logs
public final class Logger implements Serializable {

    def script

    Logger(script) {
        this.script = script
    }

    def debug(message) {
        script.println("\033[1;34m[Debug]  \033[0m ${message}")
    }

    def info(message) {
        script.println("\033[1;33m[Info]  \033[0m ${message}")
    }

    def warning(message) {
        script.println("\033[1;38;5;202m[Warning]  \033[0m ${message}")
    }

    def error(message) {
        script.println("\033[1;31m[Error]  \033[0m ${message}")
    }

    def success(message) {
        script.println("\033[1;32m[Success]  \033[0m ${message}")
    }
}

logger = new Logger(this)


// Define Initial Variables
String slaveName = "master" // TODO: Add option to run on other slave dynamically. Now hardcoded to master as there is no slave configured

// Define the pipeline parameters
properties([
        parameters([
                choice(name: 'selectedVersionType', choices: ['', 'patch', 'minor', 'major'], description: 'Choose a bump type for the version'),
                booleanParam(name: 'publishVersion', defaultValue: false, description: 'Publish the version - push the Docker image to a registry and push the changes to the repo'),
                booleanParam(name: 'loadParamsOnly', defaultValue: false, description: 'Load parameters only')
        ])
])

ansiColor('xterm') {
    if (env.BUILD_NUMBER.equals('1') || params.loadParamsOnly.toBoolean()) {
        logger.info("Loading parameters only")
        currentBuild.displayName = "#${env.BUILD_NUMBER}-parameters-loading"
        return
    }

    currentBuild.displayName = "#${env.BUILD_NUMBER}-${params.selectedVersionType}-bump"

    def curr_stage = ""

    timeout(time: 15, unit: 'MINUTES') {
        timestamps {
            node(slaveName) {
                try {
                    logger.info("Starting - selectedVersionType: ${params.selectedVersionType}, publishVersion: ${params.publishVersion}")
                    stage('Checkout app repo') {
                        curr_stage = env.STAGE_NAME
                        checkout scmGit(
                                  branches: [[name: "*/main"]], // TODO: Add option to choose another branch
                                  doGenerateSubmoduleConfigurations: false,
                                  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "."]],
                                  submoduleCfg: [],
                                  userRemoteConfigs: [[credentialsId: 'github-ssh', url: 'git@github.com:aviadbrown/PenteraAssignment.git']]
                        )
                    }

                    stage('Creating venv & Install poetry') {
                        curr_stage = env.STAGE_NAME
                        logger.info("Creating venv & Installing poetry...")
                        withPythonEnv('python3.11') { // Installing poetry in a virtual environment
                            sh "pip install poetry"
                        }
                    }

                    stage('Bump app version') {
                        curr_stage = env.STAGE_NAME
                        String prevVersion = getPoetryVersion()
                        logger.info("----------> Prev app version: ${prevVersion}")

                        if (params.selectedVersionType.equals('')) {
                            logger.info("No version bump selected, skipping the bumping process")
                            newVersion = prevVersion
                        } else {
                            // Bump the version based on the selected bump type
                            bumpPoetryVersion(params.selectedVersionType)
                            newVersion = getPoetryVersion()
                            currentBuild.displayName = "#${env.BUILD_NUMBER}-${params.selectedVersionType}-bump-to-${newVersion}"
                            logger.info("----------> New app version: ${newVersion}")
                        }
                    }

                    // Set the Docker image tag based on the bumped version
                    String dockerImageTag = "aviadbrown/pentera-web-app:${newVersion}"
                    logger.info("----------> The new Docker image tag: ${dockerImageTag}")

                    stage('Build Docker image') {
                        curr_stage = env.STAGE_NAME
                        buildDockerImage(dockerImageTag)
                    }

                    stage('Push Docker image to registry') {
                        curr_stage = env.STAGE_NAME
                        if (params.publishVersion.toBoolean()) {
                            dockerhubLogin()
                            pushDockerImage(dockerImageTag)
                        } else {
                            logger.info("Skipping the push to the registry")
                        }
                    }

                    stage('Push changes to the repo') {
                        curr_stage = env.STAGE_NAME
                        if (params.publishVersion.toBoolean()) {
                            pushToGithub()
                        } else {
                            logger.info("Skipping the push to the repo")
                        }
                    }
                    logger.success("Build completed successfully")
                    currentBuild.result = 'SUCCESS'
                } catch (Exception e) {
                    currentBuild.result = 'FAILED'
                    def errMsg = "ERROR: An error occurred in stage '${env.STAGE_NAME}': ${e}"
                    logger.error("------> ${errMsg}")
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
    withPythonEnv('python3.11') {
        sh "poetry version ${bumpType}" // Use poetry to bump the version
    }
}

String getPoetryVersion() {
    String version
    withPythonEnv('python3.11') {
        version = sh(script: "poetry version -s", returnStdout: true).trim() // Get the version from poetry
    }
    logger.debug("Poetry version: ${version}")
    return version
}

def buildDockerImage(String dockerImageTag) {
    logger.info("Building the Docker image with tag: ${dockerImageTag}")
    sh "docker build --no-cache -t ${dockerImageTag} ." // Build the Docker image - use no-cache to make sure the latest code is used
    logger.info("Docker image built successfully")
}

def dockerhubLogin() {
    withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKERHUB_PASS')]) {
        sh "echo ${DOCKERHUB_PASS} | docker login -u ${DOCKERHUB_USER} --password-stdin"
    }
}

def pushDockerImage(String dockerImageTag) {
    logger.info("Pushing the Docker image with tag: ${dockerImageTag}")
    sh "docker push ${dockerImageTag}"
    logger.info("Docker image pushed successfully")
}

def pushToGithub() {
    sshagent(credentials: ["github-ssh"]) {
        try {
            // Script to push to github - add ssh key to known hosts & set git user
            sh """
              if ! test -d ~/.ssh
              then
                 mkdir ~/.ssh
                 ssh-keyscan -t rsa,dsa github.com >> ~/.ssh/known_hosts
              fi
    
              git config user.name ${GIITHUB_USER}
              git config user.email ${GITHUB_EMAIL}
              
              git stash
              git checkout main
              
              git pull
              git stash apply --index || true
              git status
              git add .
              git commit -m 'Bump version to ${newVersion}' . || true
              git push origin main
          """
        }
        catch (err) {
            env.ERROR_MSG = err.getMessage()
            echo "ERROR MESSAGE: ${env.ERROR_MSG}"
            throw err
        }
    }
}
