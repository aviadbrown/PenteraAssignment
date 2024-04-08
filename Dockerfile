# Build base image
FROM python:3.11-slim AS base

# install poetry
RUN pip install poetry

# Set working directory
WORKDIR /app

# Copy only pyproject.toml for poetry usage to leverage build caching
COPY pyproject.toml ./

# Install python dependencies
RUN python3.11 -m venv .venv && \
    . .venv/bin/activate && \
    poetry install --no-dev --no-root

# Copy application files
COPY ./app .

# Build final image
FROM python:3.11-slim AS final

# Set working directory
WORKDIR /app

# Copy application & venv files from base image
COPY --from=base /app /app

# Set python environment variables
ENV PATH="/app/.venv/bin:$PATH"

# Expose port 8000 for the application
EXPOSE 8000

# Run the web application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
