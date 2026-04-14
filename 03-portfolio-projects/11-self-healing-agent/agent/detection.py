"""
Module 1: Detection
Executes test steps via Playwright and emits structured failure events.
"""
import logging
import time
from dataclasses import dataclass, field
from typing import Optional
from playwright.async_api import Page, Error as PlaywrightError

logger = logging.getLogger("agent.detection")


@dataclass
class TestStep:
    action: str          # "click" | "fill" | "navigate" | "assert_visible"
    locator: str         # CSS/XPath selector
    value: str = ""      # input value for "fill"
    intent: str = ""     # semantic description: "Click login button"
    timeout_ms: int = 10_000


@dataclass
class FailureEvent:
    step: TestStep
    error_type: str      # "NoSuchElement" | "Timeout" | "ActionFailed"
    error_message: str
    timestamp: float = field(default_factory=time.time)
    url: str = ""
    page_title: str = ""


async def execute_step(page: Page, step: TestStep) -> Optional[FailureEvent]:
    """Execute a single test step. Returns None on success, FailureEvent on failure."""
    try:
        loc = page.locator(step.locator)
        match step.action:
            case "click":
                await loc.click(timeout=step.timeout_ms)
            case "fill":
                await loc.fill(step.value, timeout=step.timeout_ms)
            case "navigate":
                await page.goto(step.locator, timeout=step.timeout_ms)
            case "assert_visible":
                await loc.wait_for(state="visible", timeout=step.timeout_ms)
            case _:
                raise ValueError(f"Unknown action: {step.action}")
        logger.info(f"✅ Step passed: {step.action} → {step.locator}")
        return None
    except PlaywrightError as e:
        return _classify_failure(step, e, page)
    except Exception as e:
        return _classify_failure(step, e, page)


def _classify_failure(step: TestStep, error: Exception, page: Page) -> FailureEvent:
    msg = str(error)
    if "Timeout" in msg or "waiting" in msg.lower():
        error_type = "Timeout"
    elif "not found" in msg.lower() or "no element" in msg.lower():
        error_type = "NoSuchElement"
    else:
        error_type = "ActionFailed"

    event = FailureEvent(
        step=step,
        error_type=error_type,
        error_message=msg[:500],
        url=page.url,
        page_title="",
    )
    logger.warning(f"❌ Step failed [{error_type}]: {step.action} → {step.locator}")
    return event


def detect_failure(result: Optional[FailureEvent]) -> bool:
    """Returns True if a failure was detected."""
    return result is not None
