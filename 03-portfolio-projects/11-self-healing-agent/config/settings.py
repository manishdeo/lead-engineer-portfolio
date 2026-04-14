"""
Self-Healing Agent Configuration
All settings are config-driven and overridable via environment variables.
"""
import os
from dataclasses import dataclass, field


@dataclass
class LLMConfig:
    provider: str = os.getenv("LLM_PROVIDER", "openai")  # "openai" | "ollama"
    model: str = os.getenv("LLM_MODEL", "gpt-4o")
    api_key: str = os.getenv("OPENAI_API_KEY", "")
    ollama_url: str = os.getenv("OLLAMA_URL", "http://localhost:11434")
    temperature: float = 0.2
    max_tokens: int = 1024
    timeout: int = 30


@dataclass
class BrowserConfig:
    headless: bool = os.getenv("HEADLESS", "true").lower() == "true"
    timeout_ms: int = 10_000
    screenshot_dir: str = "logs/screenshots"


@dataclass
class HealingConfig:
    max_retries: int = 3
    confidence_threshold: float = 0.6
    locator_store_path: str = "locator_store/locators.json"
    enable_learning: bool = True


@dataclass
class NotificationConfig:
    slack_webhook: str = os.getenv("SLACK_WEBHOOK_URL", "")
    firebase_url: str = os.getenv("FIREBASE_URL", "")
    enabled: bool = os.getenv("NOTIFICATIONS_ENABLED", "false").lower() == "true"


@dataclass
class AgentConfig:
    llm: LLMConfig = field(default_factory=LLMConfig)
    browser: BrowserConfig = field(default_factory=BrowserConfig)
    healing: HealingConfig = field(default_factory=HealingConfig)
    notification: NotificationConfig = field(default_factory=NotificationConfig)
    log_level: str = os.getenv("LOG_LEVEL", "INFO")
