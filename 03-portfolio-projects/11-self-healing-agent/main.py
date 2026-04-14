"""
🤖 Self-Healing Test Automation Agent
Main orchestrator implementing the control flow:
  Execute → Detect → Perceive → Reason → Act → Validate → Learn
"""
import asyncio
import logging
import sys
import time

from playwright.async_api import async_playwright, Page

from config import AgentConfig
from agent.detection import TestStep, FailureEvent, execute_step, detect_failure
from agent.perception import capture_context
from agent.context_builder import build_prompt
from agent.reasoning import call_llm, LLMResult
from agent.action import execute_action
from agent.validation import validate_success, ValidationRule
from agent.learning import learn_and_update, lookup_healed_locator
from agent.notification import notify

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s │ %(name)-22s │ %(levelname)-5s │ %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("agent.main")


async def run_test(
    steps: list[dict],
    config: AgentConfig | None = None,
    validation_rules: dict[int, ValidationRule] | None = None,
):
    """
    Execute a test as a sequence of steps with self-healing.

    Args:
        steps: List of step dicts with keys: action, locator, value, intent
        config: Agent configuration (uses defaults if None)
        validation_rules: Optional map of step_index → ValidationRule
    """
    config = config or AgentConfig()
    validation_rules = validation_rules or {}
    completed_steps: list[str] = []

    async with async_playwright() as pw:
        browser = await pw.chromium.launch(headless=config.browser.headless)
        context = await browser.new_context(viewport={"width": 1280, "height": 720})
        page = await context.new_page()

        logger.info("=" * 60)
        logger.info("🤖 SELF-HEALING AGENT STARTED")
        logger.info(f"   Steps: {len(steps)} | Max retries: {config.healing.max_retries}")
        logger.info("=" * 60)

        for i, step_dict in enumerate(steps):
            step = TestStep(**step_dict)
            logger.info(f"\n{'─' * 50}")
            logger.info(f"Step {i+1}/{len(steps)}: {step.action} → {step.locator}")
            logger.info(f"Intent: {step.intent}")

            # Check if we have a previously healed locator
            healed = lookup_healed_locator(step.locator, config.healing.locator_store_path)
            if healed:
                step = TestStep(**{**step_dict, "locator": healed})
                logger.info(f"🧠 Using cached heal: {healed}")

            # ── EXECUTE ──
            pre_url = page.url
            failure = await execute_step(page, step)

            if not detect_failure(failure):
                completed_steps.append(f"{step.action}:{step.locator}")
                continue

            # ── SELF-HEALING LOOP ──
            healed = await _healing_loop(
                page=page,
                step=TestStep(**step_dict),  # use original locator for context
                failure=failure,
                config=config,
                completed_steps=completed_steps,
                pre_url=pre_url,
                validation_rule=validation_rules.get(i),
            )

            if not healed:
                logger.error(f"🚨 UNRECOVERABLE: Step {i+1} failed after all retries")
                await notify(
                    f"🚨 Failed to heal step {i+1}: `{step.action}` → `{step.locator}`\n"
                    f"Intent: {step.intent}",
                    config.notification,
                    level="error",
                )
                break

            completed_steps.append(f"{step.action}:{step.locator}(healed)")

        logger.info(f"\n{'=' * 60}")
        logger.info(f"🏁 TEST COMPLETE: {len(completed_steps)}/{len(steps)} steps passed")
        logger.info("=" * 60)

        await browser.close()
        return len(completed_steps) == len(steps)


async def _healing_loop(
    page: Page,
    step: TestStep,
    failure: FailureEvent,
    config: AgentConfig,
    completed_steps: list[str],
    pre_url: str,
    validation_rule: ValidationRule | None,
) -> bool:
    """Core self-healing loop: Perceive → Reason → Act → Validate → Learn."""
    for attempt in range(1, config.healing.max_retries + 1):
        logger.info(f"\n🔄 Healing attempt {attempt}/{config.healing.max_retries}")

        # ── PERCEIVE ──
        ctx = await capture_context(
            page, step.locator, step.intent, completed_steps[-5:]
        )

        # ── REASON ──
        messages = build_prompt(ctx, include_screenshot=(config.llm.provider == "openai"))
        fix = await call_llm(messages, config.llm)

        if not fix or fix.confidence < config.healing.confidence_threshold:
            conf = fix.confidence if fix else 0
            logger.warning(
                f"LLM confidence too low ({conf:.2f} < {config.healing.confidence_threshold})"
            )
            continue

        logger.info(f"🧠 LLM suggests: {fix.new_locator} (confidence: {fix.confidence:.2f})")
        logger.info(f"   Reasoning: {fix.reasoning[:120]}")

        # ── ACT ──
        success = await execute_action(page, step, fix)
        if not success:
            logger.warning("Healed locator did not work, retrying...")
            continue

        # ── VALIDATE ──
        await page.wait_for_timeout(1000)  # brief settle
        valid = await validate_success(page, validation_rule, pre_url)
        if not valid:
            logger.warning("Action succeeded but validation failed, retrying...")
            continue

        # ── LEARN ──
        record = learn_and_update(step, failure, fix, config.healing.locator_store_path)

        await notify(
            f"Self-healed step: `{step.action}`\n"
            f"• Old: `{step.locator}`\n"
            f"• New: `{fix.new_locator}`\n"
            f"• Confidence: {fix.confidence:.0%}\n"
            f"• Reason: {fix.reasoning[:100]}",
            config.notification,
        )

        logger.info("✅ HEALED SUCCESSFULLY")
        return True

    return False


# ── CLI Entry Point ──
if __name__ == "__main__":
    # Example: python main.py tests/sample_test.json
    import json

    test_file = sys.argv[1] if len(sys.argv) > 1 else "tests/sample_test.json"
    with open(test_file) as f:
        test_data = json.load(f)

    success = asyncio.run(run_test(test_data["steps"]))
    sys.exit(0 if success else 1)
