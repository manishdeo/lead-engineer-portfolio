"""
Module 8: Notification
Sends real-time alerts via Slack webhook or Firebase on healing events.
"""
import logging
from typing import Optional

import httpx

from config import NotificationConfig

logger = logging.getLogger("agent.notification")


async def notify(
    message: str,
    config: NotificationConfig,
    level: str = "info",  # "info" | "warning" | "error"
):
    """Send notification to configured channels."""
    if not config.enabled:
        logger.debug(f"Notifications disabled. Message: {message}")
        return

    emoji = {"info": "✅", "warning": "⚠️", "error": "🚨"}.get(level, "ℹ️")
    formatted = f"{emoji} *Self-Healing Agent*\n{message}"

    if config.slack_webhook:
        await _send_slack(config.slack_webhook, formatted)

    if config.firebase_url:
        await _send_firebase(config.firebase_url, formatted, level)


async def _send_slack(webhook_url: str, text: str):
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.post(webhook_url, json={"text": text})
            resp.raise_for_status()
            logger.info("Slack notification sent")
    except Exception as e:
        logger.error(f"Slack notification failed: {e}")


async def _send_firebase(url: str, message: str, level: str):
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.post(
                f"{url}/healing_events.json",
                json={"message": message, "level": level},
            )
            resp.raise_for_status()
            logger.info("Firebase notification sent")
    except Exception as e:
        logger.error(f"Firebase notification failed: {e}")
