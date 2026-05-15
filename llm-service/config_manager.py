import json
import os
from typing import Optional

CONFIG_FILE = os.path.join(os.path.dirname(__file__), "config.json")

_KEY_ENV_MAP = {
    "anthropic": "ANTHROPIC_API_KEY",
    "openai":    "OPENAI_API_KEY",
    "qwen":      "QWEN_API_KEY",
}

_DEFAULT_MODELS = {
    "anthropic": "claude-sonnet-4-6",
    "openai":    "gpt-4o",
    "qwen":      "qwen-plus",
}

_config: Optional[dict] = None


def _env_defaults() -> dict:
    provider = os.getenv("PROVIDER", "anthropic").lower()
    model = os.getenv("MODEL", "") or _DEFAULT_MODELS.get(provider, "")
    api_key = os.getenv(_KEY_ENV_MAP.get(provider, ""), "")
    return {
        "provider": provider,
        "model": model,
        "apiKey": api_key,
        "mockMode": False,
        "mockScript": "MEDICATION",
    }


def _load():
    global _config
    defaults = _env_defaults()
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, "r") as f:
                stored = json.load(f)
            _config = {**defaults, **stored}
            # Keep .env API key as fallback if config.json has none
            if not _config.get("apiKey"):
                _config["apiKey"] = defaults.get("apiKey", "")
        except Exception:
            _config = defaults
    else:
        _config = defaults


def get() -> dict:
    global _config
    if _config is None:
        _load()
    return _config


def update(new_values: dict):
    global _config
    if _config is None:
        _load()
    for key, value in new_values.items():
        if key == "apiKey" and not value:
            continue  # never erase existing key with empty string
        _config[key] = value
    with open(CONFIG_FILE, "w") as f:
        json.dump(_config, f, indent=2)
    # Hot-reload llm_client so new provider/key takes effect immediately
    import llm_client
    llm_client.reload()


def safe_get() -> dict:
    """Return config with API key masked for admin display."""
    cfg = dict(get())
    key = cfg.pop("apiKey", "")
    if key and len(key) > 4:
        cfg["apiKeyMasked"] = "****..." + key[-4:]
    elif key:
        cfg["apiKeyMasked"] = "****"
    else:
        cfg["apiKeyMasked"] = ""
    return cfg
