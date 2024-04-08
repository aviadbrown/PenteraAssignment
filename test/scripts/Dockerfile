# Build base image
FROM python:3.11-slim

# install python dependencies
RUN pip install requests

# Set the working directory
WORKDIR /app

# Copy the script into the container
COPY . .
