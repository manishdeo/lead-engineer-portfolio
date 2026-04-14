"""
Module 5: Action Executor
Retries the failed action using the healed locator with fallback strategies.
"""
import logging
from typing import Optional
from playwright.async_api import Page, Error as PlaywrightError

from agent.detection import TestStep
from agent.reasoning import LLMResult

logger = logging.getLogger("agent.action")


async def execute_action(page: Page, step: TestStep, fix: LLMResult) -> bool:
    """Try the healed locator. Falls back through alternative selector strategies."""
    strategies = _build_strategies(fix)

    for i, (locator_str, strategy_name) in enumerate(strategies):
        try:
            loc = page.locator(locator_str)
            match step.action:
                case "click":
                    await loc.click(timeout=step.timeout_ms)
                case "fill":
                    await loc.fill(step.value, timeout=step.timeout_ms)
                case "assert_visible":
                    await loc.wait_for(state="visible", timeout=step.timeout_ms)
                case _:
                    await loc.click(timeout=step.timeout_ms)

            logger.info(f"🔧 Healed via [{strategy_name}]: {locator_str}")
            return True
        except PlaywrightError:
            logger.debug(f"Strategy [{strategy_name}] failed: {locator_str}")
            continue

    logger.error("All fallback strategies exhausted")
    return False


def _build_strategies(fix: LLMResult) -> list[tuple[str, str]]:
    """Build ordered list of (locator, strategy_name) to try."""
    strategies = [(fix.new_locator, f"llm_{fix.locator_type}")]

    loc = fix.new_locator
    # If LLM gave CSS, also try XPath text-based fallback
    if fix.locator_type == "css":
        # Extract possible text from reasoning for text-based fallback
        strategies.append((f"xpath={loc}" if loc.startswith("//") else loc, "css_direct"))
    elif fix.locator_type == "xpath":
        strategies.append((f"xpath={loc}", "xpath_explicit"))

    # Role-based fallback from reasoning
    if fix.locator_type == "role":
        strategies.append((loc, "role_based"))

    # Text-based last resort
    if "text=" not in loc.lower():
        # Try to extract button/link text from the locator or reasoning
        for keyword in ["Sign In", "Login", "Submit", "Continue", "Next"]:
            if keyword.lower() in fix.reasoning.lower():
                strategies.append((f"text={keyword}", "text_fallback"))
                break

    return strategies
