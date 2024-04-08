FROM python:3.11-slim

MAINTAINER "aviad.brown"

# Install poetry
RUN pip install poetry

# Set working directory
WORKDIR /app

# Copy and install application dependencies
COPY pyproject.toml ./
RUN poetry config virtualenvs.create false && \
    poetry install --no-dev --no-root

# Copy application files
COPY ./app/ .

# Expose port 8000
EXPOSE 8000

# Run the application
CMD ["uvicorn" ,"app:app", "--host", "0.0.0.0", "--port", "8000"]
