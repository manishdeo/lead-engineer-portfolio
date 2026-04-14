from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    environment: str = "development"
    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/rag_db"
    redis_url: str = "redis://localhost:6379"
    openai_api_key: str = ""
    embedding_model: str = "text-embedding-3-small"
    embedding_dimensions: int = 1536
    default_llm: str = "gpt-4-turbo"
    cheap_llm: str = "gpt-3.5-turbo"
    chunk_size: int = 512
    chunk_overlap: int = 50
    retrieval_top_k: int = 20
    rerank_top_k: int = 5
    max_conversation_history: int = 10
    semantic_cache_threshold: float = 0.95

    class Config:
        env_file = ".env"


settings = Settings()
