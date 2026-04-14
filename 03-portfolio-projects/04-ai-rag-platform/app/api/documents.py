"""Document upload and management API."""

import uuid
import tempfile
from fastapi import APIRouter, UploadFile, File, Form

from app.models.schemas import DocumentUploadResponse
from app.ingestion.chunker import load_document, chunk_document
from app.ingestion.embedder import generate_embeddings

router = APIRouter()


@router.post("/documents", response_model=DocumentUploadResponse)
async def upload_document(
    file: UploadFile = File(...),
    tenant_id: str = Form(default="default"),
):
    """
    Upload a document for RAG indexing.

    Flow:
    1. Save uploaded file temporarily
    2. Load and parse document (PDF/DOCX/TXT)
    3. Chunk into overlapping segments
    4. Generate embeddings for each chunk
    5. Store chunks + embeddings in pgvector
    """
    document_id = f"doc_{uuid.uuid4().hex[:12]}"

    # Save temp file
    with tempfile.NamedTemporaryFile(delete=False, suffix=f"_{file.filename}") as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    # Load and chunk
    pages = load_document(tmp_path, file.content_type)
    chunks = chunk_document(pages, document_id)

    # Generate embeddings
    texts = [c["content"] for c in chunks]
    embeddings = generate_embeddings(texts)

    # In production: store to PostgreSQL + pgvector via SQLAlchemy
    # For each chunk: INSERT INTO chunks (id, document_id, content, embedding, ...)

    return DocumentUploadResponse(
        document_id=document_id,
        filename=file.filename,
        status="ready",
        chunk_count=len(chunks),
    )


@router.get("/documents")
async def list_documents(tenant_id: str = "default"):
    """List all documents for a tenant."""
    # In production: query documents table filtered by tenant_id
    return {"documents": [], "tenant_id": tenant_id}


@router.delete("/documents/{document_id}")
async def delete_document(document_id: str):
    """Delete a document and all its chunks/embeddings."""
    # In production: CASCADE delete document + chunks
    return {"status": "deleted", "document_id": document_id}
