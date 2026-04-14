"""LLM Router — select the optimal model based on query complexity."""

import tiktoken

from app.config.settings import settings

COMPLEXITY_KEYWORDS = {
    "compare", "analyze", "explain why", "trade-off", "architecture",
    "design", "evaluate", "pros and cons", "deep dive", "how does",
}


def select_model(query: str, context_length: int) -> str:
    """
    Route queries to the most cost-effective model.

    Routing logic:
    - Simple queries (factual, short) → GPT-3.5 Turbo ($0.50/1M tokens)
    - Complex queries (analysis, comparison) → GPT-4 Turbo ($10/1M tokens)
    - Long context (> 16K tokens) → Claude 3 Sonnet (200K context)

    Cost savings: 60-70% by routing simple queries to cheaper models.
    """
    query_lower = query.lower()

    # Long context needs
    if context_length > 16000:
        return "claude-3-sonnet-20240229"

    # Complex query detection
    if any(kw in query_lower for kw in COMPLEXITY_KEYWORDS):
        return settings.default_llm  # gpt-4-turbo

    # Short, simple queries
    if len(query.split()) < 15:
        return settings.cheap_llm  # gpt-3.5-turbo

    return settings.default_llm


def count_tokens(text: str, model: str = "gpt-4") -> int:
    """Count tokens for a given text."""
    try:
        encoding = tiktoken.encoding_for_model(model)
    except KeyError:
        encoding = tiktoken.get_encoding("cl100k_base")
    return len(encoding.encode(text))
