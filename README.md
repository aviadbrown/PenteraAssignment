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

Jenkins plugins:
- SSH Agent: https://plugins.jenkins.io/ssh-agent/
- Pyenv Pipeline: https://plugins.jenkins.io/pyenv-pipeline/
- All other recommended plugins for jenkins

## Development Process
In order to start develop using the poetry, use this steps:
1. Clone the repo from git
2. Create a venv with python 3.11: `python3.11 -m venv .venv`
3. Activate the venv: `source .venv/bin/activate`
4. Install poetry: `pip install poetry`
5. Install dependencies: `poetry install`
6. If you want to run the app, you can use this command too: `poetry run python app/app.py`

If you need to add a package, you should use this command: `poetry add <package_name>` 

## Deployment Process
