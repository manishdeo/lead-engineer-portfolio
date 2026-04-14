import os
from langchain_community.document_loaders import PyPDFLoader
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Pinecone
from langchain_text_splitters import RecursiveCharacterTextSplitter

"""
Document Ingestion Pipeline for an AI-Powered RAG Platform.
This script loads a PDF, splits it into chunks, generates embeddings,
and stores them in a Pinecone vector database.
"""

def ingest_document(file_path: str):
    print(f"Starting ingestion for {file_path}...")

    # 1. Load Document
    loader = PyPDFLoader(file_path)
    documents = loader.load()
    print(f"Loaded {len(documents)} pages from the document.")

    # 2. Split into Chunks
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        length_function=len
    )
    chunks = text_splitter.split_documents(documents)
    print(f"Split document into {len(chunks)} chunks.")

    # 3. Initialize Embeddings Model
    embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

    # 4. Store in Vector Database (Pinecone)
    index_name = os.environ.get("PINECONE_INDEX", "rag-platform-index")
    
    Pinecone.from_documents(
        documents=chunks,
        embedding=embeddings,
        index_name=index_name
    )
    
    print(f"Successfully ingested document into Pinecone index '{index_name}'.")

if __name__ == "__main__":
    # Example usage:
    # Ensure PINECONE_API_KEY and OPENAI_API_KEY are set as environment variables.
    # You would typically run this in a background job (e.g., Celery, AWS Lambda).
    ingest_document("path/to/your/document.pdf")
