"""
Module 4: LLM/VLM Reasoning Engine
Calls OpenAI GPT-4o or local Ollama, returns structured healing suggestion.
Includes response caching for low-latency repeated failures.
"""
import hashlib
import json
import logging
from dataclasses import dataclass
from typing import Optional

import httpx

from config import LLMConfig

logger = logging.getLogger("agent.reasoning")

# Simple in-memory cache: hash(prompt_text) → LLMResult
_cache: dict[str, "LLMResult"] = {}


@dataclass
class LLMResult:
    new_locator: str
    locator_type: str   # "css" | "xpath" | "text" | "role"
    confidence: float
    reasoning: str
    raw_response: str = ""


async def call_llm(messages: list[dict], config: LLMConfig) -> Optional[LLMResult]:
    """Send prompt to LLM and parse structured JSON response."""
    cache_key = _cache_key(messages)
    if cache_key in _cache:
        logger.info("⚡ Cache hit — returning cached LLM result")
        return _cache[cache_key]

    try:
        if config.provider == "openai":
            raw = await _call_openai(messages, config)
        else:
            raw = await _call_ollama(messages, config)

        result = _parse_response(raw)
        if result:
            _cache[cache_key] = result
        return result
    except Exception as e:
        logger.error(f"LLM call failed: {e}")
        return None


async def _call_openai(messages: list[dict], config: LLMConfig) -> str:
    async with httpx.AsyncClient(timeout=config.timeout) as client:
        resp = await client.post(
            "https://api.openai.com/v1/chat/completions",
            headers={"Authorization": f"Bearer {config.api_key}"},
            json={
                "model": config.model,
                "messages": messages,
                "temperature": config.temperature,
                "max_tokens": config.max_tokens,
            },
        )
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]


async def _call_ollama(messages: list[dict], config: LLMConfig) -> str:
    # Ollama doesn't support vision content arrays — flatten to text-only
    flat_messages = []
    for m in messages:
        content = m["content"]
        if isinstance(content, list):
            text_parts = [p["text"] for p in content if p.get("type") == "text"]
            content = "\n".join(text_parts)
        flat_messages.append({"role": m["role"], "content": content})

    async with httpx.AsyncClient(timeout=config.timeout) as client:
        resp = await client.post(
            f"{config.ollama_url}/api/chat",
            json={"model": config.model, "messages": flat_messages, "stream": False},
        )
        resp.raise_for_status()
        return resp.json()["message"]["content"]


def _parse_response(raw: str) -> Optional[LLMResult]:
    """Extract JSON from LLM response (handles markdown fences)."""
    text = raw.strip()
    if "```" in text:
        text = text.split("```")[1]
        if text.startswith("json"):
            text = text[4:]
        text = text.strip()

    try:
        data = json.loads(text)
        return LLMResult(
            new_locator=data["new_locator"],
            locator_type=data.get("locator_type", "css"),
            confidence=float(data.get("confidence", 0)),
            reasoning=data.get("reasoning", ""),
            raw_response=raw,
        )
    except (json.JSONDecodeError, KeyError) as e:
        logger.error(f"Failed to parse LLM response: {e}\nRaw: {raw[:300]}")
        return None


def _cache_key(messages: list[dict]) -> str:
    # Hash only the text portions (exclude screenshot for cache efficiency)
    text_parts = []
    for m in messages:
        c = m["content"]
        if isinstance(c, list):
            text_parts.extend(p.get("text", "") for p in c if p.get("type") == "text")
        else:
            text_parts.append(str(c))
    return hashlib.sha256("".join(text_parts).encode()).hexdigest()[:16]
