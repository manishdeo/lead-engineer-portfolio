"""Document chunking with recursive text splitting."""

from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import PyPDFLoader, Docx2txtLoader, TextLoader

from app.config.settings import settings


def load_document(file_path: str, content_type: str) -> list[dict]:
    """Load document based on content type."""
    loaders = {
        "application/pdf": PyPDFLoader,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": Docx2txtLoader,
        "text/plain": TextLoader,
        "text/markdown": TextLoader,
    }
    loader_cls = loaders.get(content_type, TextLoader)
    loader = loader_cls(file_path)
    return loader.load()


def chunk_document(pages: list, document_id: str) -> list[dict]:
    """
    Split document into chunks using recursive text splitting.

    Strategy:
    - chunk_size=512 tokens — balances context richness vs retrieval precision
    - chunk_overlap=50 tokens (10%) — preserves context at boundaries
    - Separators prioritize paragraph > sentence > word boundaries
    """
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=settings.chunk_size,
        chunk_overlap=settings.chunk_overlap,
        separators=["\n\n", "\n", ". ", ", ", " ", ""],
        length_function=len,
    )

    chunks = []
    for i, page in enumerate(pages):
        page_chunks = splitter.split_text(page.page_content)
        for j, chunk_text in enumerate(page_chunks):
            chunks.append({
                "content": chunk_text,
                "page_number": i + 1,
                "chunk_index": len(chunks),
                "document_id": document_id,
                "metadata": {
                    "source": document_id,
                    "page": i + 1,
                },
            })

    return chunks
