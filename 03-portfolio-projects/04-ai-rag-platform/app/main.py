from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.api import chat, documents, health
from app.config.settings import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize DB, Redis, embedding model
    print(f"Starting RAG Platform on {settings.environment}")
    yield
    # Shutdown: cleanup connections
    print("Shutting down RAG Platform")


app = FastAPI(
    title="AI RAG Platform",
    version="1.0.0",
    description="Production-ready Retrieval-Augmented Generation API",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router, tags=["Health"])
app.include_router(documents.router, prefix="/api/v1", tags=["Documents"])
app.include_router(chat.router, prefix="/api/v1", tags=["Chat"])
