from datetime import datetime
from pydantic import BaseModel, Field
from sqlalchemy import Column, String, Integer, DateTime, Text, ForeignKey
from sqlalchemy.orm import DeclarativeBase, relationship
from pgvector.sqlalchemy import Vector

from app.config.settings import settings


class Base(DeclarativeBase):
    pass


class Document(Base):
    __tablename__ = "documents"
    id = Column(String, primary_key=True)
    tenant_id = Column(String, nullable=False, index=True)
    filename = Column(String, nullable=False)
    content_type = Column(String)
    page_count = Column(Integer, default=0)
    chunk_count = Column(Integer, default=0)
    status = Column(String, default="processing")  # processing, ready, failed
    created_at = Column(DateTime, default=datetime.utcnow)
    chunks = relationship("Chunk", back_populates="document", cascade="all, delete-orphan")


class Chunk(Base):
    __tablename__ = "chunks"
    id = Column(String, primary_key=True)
    document_id = Column(String, ForeignKey("documents.id"), nullable=False)
    content = Column(Text, nullable=False)
    page_number = Column(Integer)
    chunk_index = Column(Integer)
    embedding = Column(Vector(settings.embedding_dimensions))
    metadata_ = Column("metadata", Text)  # JSON string
    document = relationship("Document", back_populates="chunks")


class Conversation(Base):
    __tablename__ = "conversations"
    id = Column(String, primary_key=True)
    tenant_id = Column(String, nullable=False, index=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class Message(Base):
    __tablename__ = "messages"
    id = Column(String, primary_key=True)
    conversation_id = Column(String, ForeignKey("conversations.id"), nullable=False)
    role = Column(String, nullable=False)  # user, assistant
    content = Column(Text, nullable=False)
    sources = Column(Text)  # JSON array of source references
    created_at = Column(DateTime, default=datetime.utcnow)


# --- Pydantic Schemas ---

class DocumentUploadResponse(BaseModel):
    document_id: str
    filename: str
    status: str
    chunk_count: int = 0


class ChatRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=2000)
    conversation_id: str | None = None
    tenant_id: str = "default"


class ChatResponse(BaseModel):
    answer: str
    sources: list[dict]
    conversation_id: str
    model_used: str


class SourceReference(BaseModel):
    document_id: str
    filename: str
    page_number: int | None
    chunk_content: str
    relevance_score: float
