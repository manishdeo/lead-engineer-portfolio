"""
Module 7: Learning
Persists healed locators, logs healing history, and optionally updates test files.
"""
import json
import logging
import os
import time
from typing import Optional

from agent.detection import TestStep, FailureEvent
from agent.reasoning import LLMResult

logger = logging.getLogger("agent.learning")

DEFAULT_STORE = "locator_store/locators.json"
HISTORY_LOG = "logs/healing_history.jsonl"


def learn_and_update(
    step: TestStep,
    failure: FailureEvent,
    fix: LLMResult,
    store_path: str = DEFAULT_STORE,
) -> dict:
    """Record the healing event and update the locator store."""
    record = {
        "timestamp": time.time(),
        "original_locator": step.locator,
        "new_locator": fix.new_locator,
        "locator_type": fix.locator_type,
        "confidence": fix.confidence,
        "intent": step.intent,
        "action": step.action,
        "error_type": failure.error_type,
        "reasoning": fix.reasoning,
        "url": failure.url,
    }

    # Update locator store (JSON mapping: old_locator → best known locator)
    _update_store(step.locator, fix, store_path)

    # Append to healing history log
    _append_history(record)

    logger.info(
        f"📚 Learned: '{step.locator}' → '{fix.new_locator}' "
        f"(confidence: {fix.confidence:.2f})"
    )
    return record


def lookup_healed_locator(original: str, store_path: str = DEFAULT_STORE) -> Optional[str]:
    """Check if we already have a healed locator for this selector."""
    store = _load_store(store_path)
    entry = store.get(original)
    if entry:
        logger.info(f"🧠 Found cached heal: '{original}' → '{entry['new_locator']}'")
        return entry["new_locator"]
    return None


def _update_store(old_locator: str, fix: LLMResult, store_path: str):
    store = _load_store(store_path)
    existing = store.get(old_locator)
    # Only update if new fix has higher confidence
    if not existing or fix.confidence > existing.get("confidence", 0):
        store[old_locator] = {
            "new_locator": fix.new_locator,
            "locator_type": fix.locator_type,
            "confidence": fix.confidence,
            "updated_at": time.time(),
        }
        os.makedirs(os.path.dirname(store_path), exist_ok=True)
        with open(store_path, "w") as f:
            json.dump(store, f, indent=2)


def _load_store(path: str) -> dict:
    if os.path.exists(path):
        with open(path) as f:
            return json.load(f)
    return {}


def _append_history(record: dict):
    os.makedirs(os.path.dirname(HISTORY_LOG), exist_ok=True)
    with open(HISTORY_LOG, "a") as f:
        f.write(json.dumps(record) + "\n")
