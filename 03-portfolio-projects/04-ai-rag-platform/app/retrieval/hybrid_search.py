"""Hybrid search combining pgvector cosine similarity with BM25 keyword matching."""

from sqlalchemy import text
from app.ingestion.embedder import generate_query_embedding
from app.config.settings import settings


async def hybrid_search(db_session, query: str, tenant_id: str, top_k: int = None) -> list[dict]:
    """
    Hybrid search: Vector similarity (semantic) + BM25 keyword (lexical).

    Why hybrid?
    - Vector search catches semantic similarity ("car" matches "automobile")
    - BM25 catches exact terms, acronyms, proper nouns ("AWS EKS" matches "EKS")
    - Combined score = 0.7 * vector_score + 0.3 * bm25_score

    Returns top_k candidates for re-ranking.
    """
    top_k = top_k or settings.retrieval_top_k
    query_embedding = generate_query_embedding(query)

    # Hybrid query: pgvector cosine distance + pg_trgm similarity
    sql = text("""
        WITH vector_results AS (
            SELECT
                c.id,
                c.content,
                c.page_number,
                c.document_id,
                d.filename,
                1 - (c.embedding <=> :embedding::vector) AS vector_score
            FROM chunks c
            JOIN documents d ON c.document_id = d.id
            WHERE d.tenant_id = :tenant_id AND d.status = 'ready'
            ORDER BY c.embedding <=> :embedding::vector
            LIMIT :limit
        ),
        keyword_results AS (
            SELECT
                c.id,
                c.content,
                c.page_number,
                c.document_id,
                d.filename,
                similarity(c.content, :query) AS keyword_score
            FROM chunks c
            JOIN documents d ON c.document_id = d.id
            WHERE d.tenant_id = :tenant_id AND d.status = 'ready'
              AND c.content %% :query
            ORDER BY similarity(c.content, :query) DESC
            LIMIT :limit
        )
        SELECT
            COALESCE(v.id, k.id) AS id,
            COALESCE(v.content, k.content) AS content,
            COALESCE(v.page_number, k.page_number) AS page_number,
            COALESCE(v.document_id, k.document_id) AS document_id,
            COALESCE(v.filename, k.filename) AS filename,
            COALESCE(v.vector_score, 0) AS vector_score,
            COALESCE(k.keyword_score, 0) AS keyword_score,
            (0.7 * COALESCE(v.vector_score, 0) + 0.3 * COALESCE(k.keyword_score, 0)) AS combined_score
        FROM vector_results v
        FULL OUTER JOIN keyword_results k ON v.id = k.id
        ORDER BY combined_score DESC
        LIMIT :limit
    """)

    result = await db_session.execute(sql, {
        "embedding": str(query_embedding),
        "query": query,
        "tenant_id": tenant_id,
        "limit": top_k,
    })

    return [
        {
            "id": row.id,
            "content": row.content,
            "page_number": row.page_number,
            "document_id": row.document_id,
            "filename": row.filename,
            "vector_score": float(row.vector_score),
            "keyword_score": float(row.keyword_score),
            "combined_score": float(row.combined_score),
        }
        for row in result.fetchall()
    ]
