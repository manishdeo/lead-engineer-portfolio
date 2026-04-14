"""Embedding generation using OpenAI text-embedding-3-small."""

from openai import OpenAI

from app.config.settings import settings

client = OpenAI(api_key=settings.openai_api_key)


def generate_embeddings(texts: list[str]) -> list[list[float]]:
    """
    Generate embeddings for a batch of texts.

    Uses text-embedding-3-small (1536 dims, $0.02/1M tokens).
    Batches up to 2048 texts per API call.
    """
    embeddings = []
    batch_size = 2048

    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        response = client.embeddings.create(
            model=settings.embedding_model,
            input=batch,
        )
        embeddings.extend([item.embedding for item in response.data])

    return embeddings


def generate_query_embedding(query: str) -> list[float]:
    """Generate embedding for a single query."""
    response = client.embeddings.create(
        model=settings.embedding_model,
        input=query,
    )
    return response.data[0].embedding
