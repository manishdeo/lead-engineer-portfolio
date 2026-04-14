"""
Module 2: Perception
Captures DOM snapshot, screenshot, and page metadata for LLM reasoning.
"""
import base64
import logging
import os
import time
from dataclasses import dataclass
from typing import Optional
from playwright.async_api import Page

logger = logging.getLogger("agent.perception")

SCREENSHOT_DIR = "logs/screenshots"


@dataclass
class PageContext:
    url: str
    title: str
    dom_snapshot: str          # trimmed HTML
    screenshot_b64: str        # base64-encoded PNG
    screenshot_path: str
    failed_locator: str
    intent: str
    previous_steps: list


async def capture_context(
    page: Page,
    failed_locator: str,
    intent: str,
    previous_steps: Optional[list] = None,
) -> PageContext:
    """Capture full page context for LLM analysis."""
    os.makedirs(SCREENSHOT_DIR, exist_ok=True)

    url = page.url
    title = await page.title()

    # DOM: grab body innerHTML, trim to 15k chars to stay within token limits
    dom = await page.evaluate("document.documentElement.outerHTML")
    dom_trimmed = _trim_dom(dom, max_chars=15_000)

    # Screenshot
    ts = int(time.time())
    path = os.path.join(SCREENSHOT_DIR, f"failure_{ts}.png")
    screenshot_bytes = await page.screenshot(full_page=False)
    with open(path, "wb") as f:
        f.write(screenshot_bytes)
    screenshot_b64 = base64.b64encode(screenshot_bytes).decode()

    logger.info(f"📸 Context captured: {url} | DOM {len(dom_trimmed)} chars")

    return PageContext(
        url=url,
        title=title,
        dom_snapshot=dom_trimmed,
        screenshot_b64=screenshot_b64,
        screenshot_path=path,
        failed_locator=failed_locator,
        intent=intent,
        previous_steps=previous_steps or [],
    )


def _trim_dom(html: str, max_chars: int = 15_000) -> str:
    """Remove scripts/styles and trim DOM to fit LLM context window."""
    import re
    html = re.sub(r"<script[\s\S]*?</script>", "", html, flags=re.IGNORECASE)
    html = re.sub(r"<style[\s\S]*?</style>", "", html, flags=re.IGNORECASE)
    html = re.sub(r"<!--[\s\S]*?-->", "", html)
    html = re.sub(r"\s{2,}", " ", html)
    return html[:max_chars]
