"""
Module 3: Context Builder
Constructs structured LLM prompt payload from captured page context.
"""
import json
import logging
from agent.perception import PageContext

logger = logging.getLogger("agent.context_builder")

SYSTEM_PROMPT = """\
You are an expert test automation engineer AI. A UI test step has failed because the \
element locator no longer matches any element on the page.

Your job:
1. Analyze the DOM snapshot and screenshot.
2. Understand the INTENT of the failed step.
3. Find the best matching element in the current DOM.
4. Return a new locator that fulfills the original intent.

Rules:
- Prefer CSS selectors. Fall back to XPath only if CSS is ambiguous.
- Consider text content, ARIA roles, data-testid, name, placeholder attributes.
- Return confidence 0.0-1.0 based on how certain you are.
- If no match exists, return confidence 0 and explain why.

Respond ONLY with valid JSON:
{
  "new_locator": "...",
  "locator_type": "css" | "xpath" | "text" | "role",
  "confidence": 0.0,
  "reasoning": "..."
}"""


def build_prompt(ctx: PageContext, include_screenshot: bool = True) -> dict:
    """Build the messages payload for the LLM call."""
    user_content = []

    # Text part
    text_payload = {
        "intent": ctx.intent,
        "failed_locator": ctx.failed_locator,
        "page_url": ctx.url,
        "page_title": ctx.title,
        "previous_steps": ctx.previous_steps[-5:],  # last 5 for context
        "dom_snippet": ctx.dom_snapshot,
    }
    user_content.append({
        "type": "text",
        "text": f"Failed test step context:\n```json\n{json.dumps(text_payload, indent=2)}\n```\n\n"
                "Analyze the DOM (and screenshot if provided) and return the corrected locator.",
    })

    # Vision part (optional, for GPT-4o / VLM)
    if include_screenshot and ctx.screenshot_b64:
        user_content.append({
            "type": "image_url",
            "image_url": {"url": f"data:image/png;base64,{ctx.screenshot_b64}", "detail": "low"},
        })

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_content},
    ]

    logger.info("🧩 Prompt built with %d content parts", len(user_content))
    return messages
