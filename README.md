# PenteraAssignment Docs

This is assignment to build and deploy a small python web app in docker container.
The python app versioning and dependencies managed by poetry. Docs can be found here: https://python-poetry.org/docs/

Pre-requisites:
- Docker
- Python 3.11

python pre-requisites:
- Fastapi
- Uvicorn
- poetry

Jenkins plugins & pre-requisites:
- SSH Agent: https://plugins.jenkins.io/ssh-agent/
- Pyenv Pipeline: https://plugins.jenkins.io/pyenv-pipeline/
- All other recommended plugins for jenkins
- Configure required secrets & env vars (like dockerhub credentials, git credentials, etc.).

## Development Process
In order to start develop using poetry, use this steps:
1. Clone the repo from git
2. Create a venv with python 3.11: `python3.11 -m venv .venv`
3. Activate the venv: `source .venv/bin/activate`
4. Install poetry: `pip install poetry`
5. Install dependencies: `poetry install`
6. If you want to run the app, you can use this command too: `poetry run python app/app.py`

* Poetry file is located in the root of the project: `pyproject.toml`.
It contains the dependencies and the app version.
* If you need to add a package, you should use this command: `poetry add <package_name>`

## Deployment Process
The final deployment process done within a Jenkins pipeline called app-builder.
The pipeline stages:
1. Checkout the code from git.
2. Bump the version of the app with the required bumping using poetry - major, minor, or patch. If you choose empty (default), override the current version (use for rebuild after failure or for hotfix without releasing a new version).
3. Build the docker image with the new version as the tag.
4. Push the docker image to the docker hub if the flag `publishVersion` is true.
5. Push the version bumping changes to git if the flag `publishVersion` is true.

* The docker image sits in dockerhub, in the public repo `aviadbrown/pentera-web-app:tagname`: https://hub.docker.com/repository/docker/aviadbrown/pentera-web-app/general
* The Dockerfile created with multi-stage build, in order to reduce the image size.
* There is another Dockerfile-one-stage in the root of the project, which is used for comparison. Multi-stage image size is 198MB while the one-stage image size is 259GB. Functionality is the same.

## Testing Deployment
In order to test the deployment, you can use the following steps:
1. Clone this repo.
2. In terminal, enter to test folder: `cd test`
3. Run the following command: `IMAGE_TAG={required_version} docker-compose up`
4. In terminal you should see the log in console (wait few seconds): 
`test-1     | API call successful (HTTP 200 OK) and output is correct
test-1 exited with code 0`
5. If you see the above message, the deployment is successful. Otherwise, there is a failure in the deployment.

TODO:
1. Get rid of all hardcoded values.