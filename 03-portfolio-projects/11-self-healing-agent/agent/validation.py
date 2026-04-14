"""
Module 6: Validation
Verifies the healed action succeeded via URL change, DOM state, or element visibility.
"""
import logging
from dataclasses import dataclass
from typing import Optional
from playwright.async_api import Page

logger = logging.getLogger("agent.validation")


@dataclass
class ValidationRule:
    """Define what 'success' looks like after a healed step."""
    expected_url_contains: Optional[str] = None
    expected_element_visible: Optional[str] = None  # selector
    expected_title_contains: Optional[str] = None
    dom_text_contains: Optional[str] = None


async def validate_success(
    page: Page,
    rule: Optional[ValidationRule] = None,
    pre_url: str = "",
) -> bool:
    """Run validation checks. Returns True if at least one check passes."""
    if rule is None:
        # Default: check if URL changed (navigation happened)
        return page.url != pre_url

    checks_passed = 0
    checks_run = 0

    if rule.expected_url_contains:
        checks_run += 1
        if rule.expected_url_contains in page.url:
            checks_passed += 1
            logger.info(f"✅ URL check passed: {page.url}")

    if rule.expected_element_visible:
        checks_run += 1
        try:
            await page.locator(rule.expected_element_visible).wait_for(
                state="visible", timeout=5000
            )
            checks_passed += 1
            logger.info(f"✅ Element visible: {rule.expected_element_visible}")
        except Exception:
            logger.debug(f"Element not visible: {rule.expected_element_visible}")

    if rule.expected_title_contains:
        checks_run += 1
        title = await page.title()
        if rule.expected_title_contains.lower() in title.lower():
            checks_passed += 1
            logger.info(f"✅ Title check passed: {title}")

    if rule.dom_text_contains:
        checks_run += 1
        body = await page.inner_text("body")
        if rule.dom_text_contains.lower() in body.lower():
            checks_passed += 1
            logger.info(f"✅ DOM text check passed")

    passed = checks_passed > 0
    logger.info(f"Validation: {checks_passed}/{checks_run} checks passed → {'✅' if passed else '❌'}")
    return passed
