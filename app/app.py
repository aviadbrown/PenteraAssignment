from contextlib import asynccontextmanager

from fastapi import FastAPI
import uvicorn
import logging

logging.basicConfig(level=logging.INFO)


# This is a method that will be called when the application starts
@asynccontextmanager
async def lifespan(app: FastAPI):
	logging.info("Application started successfully!")
	yield


app = FastAPI(lifespan=lifespan)


@app.get("/")
def root_response():
	return "Hello, DevOps!"


if __name__ == "__main__":
	uvicorn.run(app, host="0.0.0.0", port=8000)
