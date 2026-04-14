"""Cross-encoder re-ranking for improved retrieval precision."""

from sentence_transformers import CrossEncoder

from app.config.settings import settings

# Load cross-encoder model (cached after first load)
_reranker = None


def get_reranker():
    global _reranker
    if _reranker is None:
        _reranker = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2", max_length=512)
    return _reranker


def rerank(query: str, candidates: list[dict], top_k: int = None) -> list[dict]:
    """
    Re-rank retrieved candidates using a cross-encoder.

    Why re-rank?
    - Bi-encoder (embedding) is fast but less accurate for ranking
    - Cross-encoder processes (query, document) pairs jointly — more accurate
    - Use bi-encoder for recall (top 20), cross-encoder for precision (top 5)

    This is the standard two-stage retrieval pattern used in production RAG.
    """
    top_k = top_k or settings.rerank_top_k

    if not candidates:
        return []

    reranker = get_reranker()
    pairs = [(query, c["content"]) for c in candidates]
    scores = reranker.predict(pairs)

    for i, score in enumerate(scores):
        candidates[i]["rerank_score"] = float(score)

    reranked = sorted(candidates, key=lambda x: x["rerank_score"], reverse=True)
    return reranked[:top_k]
