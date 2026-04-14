# 🤖 Self-Healing Test Automation Agent

> An AI-powered test automation agent inspired by [Skyvern](https://github.com/Skyvern-AI/skyvern) that transforms rigid test scripts into adaptive, self-correcting systems using LLM/VLM reasoning.

When a UI locator breaks, the agent **detects** the failure, **perceives** the page state, **reasons** about the fix using GPT-4o or a local LLM, **acts** with the healed locator, **validates** success, and **learns** permanently — all in real-time.

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SELF-HEALING AGENT LOOP                       │
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ EXECUTE  │───▶│ DETECT   │───▶│ PERCEIVE │───▶│ REASON   │  │
│  │ Step     │    │ Failure  │    │ Context  │    │ (LLM)    │  │
│  └──────────┘    └────┬─────┘    └──────────┘    └────┬─────┘  │
│       ▲               │ success       │                │        │
│       │               ▼               │                ▼        │
│       │          ┌─────────┐          │         ┌──────────┐   │
│       │          │ CONTINUE│          │         │ ACT      │   │
│       │          │ Next    │          │         │ Retry w/ │   │
│       │          └─────────┘          │         │ new loc  │   │
│       │                               │         └────┬─────┘   │
│       │                               │              │         │
│       │                               │              ▼         │
│       │                               │        ┌──────────┐   │
│       │                               │        │ VALIDATE │   │
│       │                               │        │ Success? │   │
│       │                               │        └────┬─────┘   │
│       │                               │              │         │
│       │                               │         yes  │  no     │
│       │                               │         ┌────┴────┐   │
│       │                               │         ▼         ▼   │
│       │                          ┌─────────┐  ┌──────┐       │
│       └──────────────────────────│ LEARN   │  │RETRY │       │
│                                  │ & Store │  │/ESC  │       │
│                                  └────┬────┘  └──────┘       │
│                                       │                       │
│                                       ▼                       │
│                                  ┌─────────┐                  │
│                                  │ NOTIFY  │                  │
│                                  │ Slack/  │                  │
│                                  │ Firebase│                  │
│                                  └─────────┘                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       MODULE MAP                                │
├──────────────────┬──────────────────────────────────────────────┤
│ detection.py     │ Execute steps, classify failures             │
│ perception.py    │ DOM snapshot, screenshot, page metadata      │
│ context_builder  │ Structured LLM prompt construction           │
│ reasoning.py     │ OpenAI GPT-4o / Ollama integration + cache   │
│ action.py        │ Retry with healed locator + fallbacks        │
│ validation.py    │ URL/DOM/visibility success verification      │
│ learning.py      │ Persist healed locators, history log         │
│ notification.py  │ Slack / Firebase real-time alerts            │
├──────────────────┼──────────────────────────────────────────────┤
│ main.py          │ Orchestrator — ties all modules together     │
│ config/          │ Dataclass-based, env-var-driven settings     │
│ locator_store/   │ JSON persistence for learned locators        │
│ prompts/         │ LLM prompt templates & engineering notes     │
└──────────────────┴──────────────────────────────────────────────┘
```

---

## ⚡ Quick Start

### 1. Install Dependencies

```bash
cd 03-portfolio-projects/11-self-healing-agent
pip install -r requirements.txt
playwright install chromium
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your OpenAI API key (or switch to Ollama)
```

### 3. Run the Sample Test

```bash
# Working test (all locators correct)
python main.py tests/sample_test.json

# Broken test (triggers self-healing)
python main.py tests/broken_test.json
```

### 4. Using Ollama (Local LLM)

```bash
# Install Ollama: https://ollama.ai
ollama pull llama3.1
export LLM_PROVIDER=ollama
export LLM_MODEL=llama3.1
python main.py tests/broken_test.json
```

---

## 🧪 Example Run Output

```
08:30:01 │ agent.main             │ INFO  │ ============================================================
08:30:01 │ agent.main             │ INFO  │ 🤖 SELF-HEALING AGENT STARTED
08:30:01 │ agent.main             │ INFO  │    Steps: 5 | Max retries: 3
08:30:01 │ agent.main             │ INFO  │ ============================================================
08:30:01 │ agent.main             │ INFO  │
08:30:01 │ agent.main             │ INFO  │ ──────────────────────────────────────────────────
08:30:01 │ agent.main             │ INFO  │ Step 1/5: navigate → https://practicetestautomation.com/practice-test-login/
08:30:01 │ agent.main             │ INFO  │ Intent: Navigate to the login page
08:30:02 │ agent.detection        │ INFO  │ ✅ Step passed: navigate → https://...
08:30:02 │ agent.main             │ INFO  │
08:30:02 │ agent.main             │ INFO  │ ──────────────────────────────────────────────────
08:30:02 │ agent.main             │ INFO  │ Step 2/5: fill → #user-email-input
08:30:02 │ agent.main             │ INFO  │ Intent: Enter username into the username input field
08:30:12 │ agent.detection        │ WARN  │ ❌ Step failed [Timeout]: fill → #user-email-input
08:30:12 │ agent.main             │ INFO  │
08:30:12 │ agent.main             │ INFO  │ 🔄 Healing attempt 1/3
08:30:12 │ agent.perception       │ INFO  │ 📸 Context captured: https://...login/ | DOM 4521 chars
08:30:13 │ agent.context_builder  │ INFO  │ 🧩 Prompt built with 2 content parts
08:30:15 │ agent.reasoning        │ INFO  │ LLM responded in 2.1s
08:30:15 │ agent.main             │ INFO  │ 🧠 LLM suggests: #username (confidence: 0.95)
08:30:15 │ agent.main             │ INFO  │    Reasoning: The DOM contains an input with id="username" and placeholder="Username"...
08:30:15 │ agent.action           │ INFO  │ 🔧 Healed via [llm_css]: #username
08:30:16 │ agent.validation       │ INFO  │ ✅ URL check passed
08:30:16 │ agent.learning         │ INFO  │ 📚 Learned: '#user-email-input' → '#username' (confidence: 0.95)
08:30:16 │ agent.main             │ INFO  │ ✅ HEALED SUCCESSFULLY
08:30:16 │ agent.main             │ INFO  │
08:30:16 │ agent.main             │ INFO  │ ──────────────────────────────────────────────────
08:30:16 │ agent.main             │ INFO  │ Step 3/5: fill → #pass-field
08:30:26 │ agent.detection        │ WARN  │ ❌ Step failed [Timeout]: fill → #pass-field
08:30:26 │ agent.main             │ INFO  │ 🔄 Healing attempt 1/3
08:30:28 │ agent.main             │ INFO  │ 🧠 LLM suggests: #password (confidence: 0.95)
08:30:28 │ agent.action           │ INFO  │ 🔧 Healed via [llm_css]: #password
08:30:28 │ agent.learning         │ INFO  │ 📚 Learned: '#pass-field' → '#password' (confidence: 0.95)
08:30:28 │ agent.main             │ INFO  │ ✅ HEALED SUCCESSFULLY
08:30:28 │ agent.main             │ INFO  │
08:30:28 │ agent.main             │ INFO  │ ──────────────────────────────────────────────────
08:30:28 │ agent.main             │ INFO  │ Step 4/5: click → #login-btn
08:30:28 │ agent.detection        │ WARN  │ ❌ Step failed [Timeout]: click → #login-btn
08:30:30 │ agent.main             │ INFO  │ 🧠 LLM suggests: #submit (confidence: 0.92)
08:30:30 │ agent.main             │ INFO  │    Reasoning: The submit button has id="submit" with text "Submit"
08:30:30 │ agent.action           │ INFO  │ 🔧 Healed via [llm_css]: #submit
08:30:31 │ agent.validation       │ INFO  │ ✅ URL check passed: .../logged-in-successfully/
08:30:31 │ agent.learning         │ INFO  │ 📚 Learned: '#login-btn' → '#submit' (confidence: 0.92)
08:30:31 │ agent.main             │ INFO  │ ✅ HEALED SUCCESSFULLY
08:30:31 │ agent.main             │ INFO  │
08:30:31 │ agent.main             │ INFO  │ ──────────────────────────────────────────────────
08:30:31 │ agent.main             │ INFO  │ Step 5/5: assert_visible → h1.dashboard-title
08:30:41 │ agent.detection        │ WARN  │ ❌ Step failed [Timeout]: assert_visible → h1.dashboard-title
08:30:43 │ agent.main             │ INFO  │ 🧠 LLM suggests: .post-title (confidence: 0.88)
08:30:43 │ agent.action           │ INFO  │ 🔧 Healed via [llm_css]: .post-title
08:30:43 │ agent.learning         │ INFO  │ 📚 Learned: 'h1.dashboard-title' → '.post-title' (confidence: 0.88)
08:30:43 │ agent.main             │ INFO  │ ✅ HEALED SUCCESSFULLY
08:30:43 │ agent.main             │ INFO  │
08:30:43 │ agent.main             │ INFO  │ ============================================================
08:30:43 │ agent.main             │ INFO  │ 🏁 TEST COMPLETE: 5/5 steps passed
08:30:43 │ agent.main             │ INFO  │ ============================================================
```

### Locator Store After Run

```json
// locator_store/locators.json
{
  "#user-email-input": {
    "new_locator": "#username",
    "locator_type": "css",
    "confidence": 0.95,
    "updated_at": 1706000000.0
  },
  "#pass-field": {
    "new_locator": "#password",
    "locator_type": "css",
    "confidence": 0.95,
    "updated_at": 1706000001.0
  },
  "#login-btn": {
    "new_locator": "#submit",
    "locator_type": "css",
    "confidence": 0.92,
    "updated_at": 1706000002.0
  }
}
```

---

## 📁 Project Structure

```
11-self-healing-agent/
├── main.py                    # Orchestrator & CLI entry point
├── requirements.txt
├── .env.example
│
├── agent/                     # Core modules
│   ├── __init__.py
│   ├── detection.py           # Step execution & failure classification
│   ├── perception.py          # DOM/screenshot/metadata capture
│   ├── context_builder.py     # LLM prompt construction
│   ├── reasoning.py           # GPT-4o / Ollama integration + cache
│   ├── action.py              # Healed locator execution + fallbacks
│   ├── validation.py          # Post-action success verification
│   ├── learning.py            # Locator store & history persistence
│   └── notification.py        # Slack / Firebase alerts
│
├── config/                    # Configuration
│   ├── __init__.py
│   └── settings.py            # Dataclass-based config
│
├── prompts/                   # LLM prompt templates
│   └── PROMPT_TEMPLATES.md
│
├── tests/                     # Test definitions
│   ├── sample_test.json       # Working test
│   └── broken_test.json       # Broken locators (triggers healing)
│
├── locator_store/             # Persistent learned locators
│   └── locators.json
│
├── logs/                      # Runtime artifacts
│   ├── screenshots/           # Failure screenshots
│   └── healing_history.jsonl  # Healing event log
│
└── docs/
    └── visualization.html     # Animated healing flow demo
```

---

## 🎨 Visualization (Demo Dashboard)

Open `docs/visualization.html` in a browser for an animated flow showing:

```
  ❌ FAILURE          🧠 AI THINKING         🔧 FIX            ✅ SUCCESS
  ┌─────────┐        ┌─────────────┐       ┌─────────┐       ┌─────────┐
  │ #login  │  ───▶  │  Analyzing  │ ───▶  │ #submit │ ───▶  │Dashboard│
  │ -btn    │        │  DOM + IMG  │       │  found  │       │ loaded! │
  │ NOT     │        │  ...        │       │ conf:92%│       │         │
  │ FOUND   │        │  💭         │       │         │       │  🎉     │
  └─────────┘        └─────────────┘       └─────────┘       └─────────┘
```

---

## 🔮 Future Enhancements

| Enhancement | Description |
|---|---|
| **GitHub PR Generation** | Auto-create PRs with healed locators using PyGithub |
| **Visual Regression** | Compare pre/post screenshots to detect layout shifts |
| **Multi-browser Support** | Firefox, WebKit via Playwright's multi-browser API |
| **Parallel Healing** | Heal multiple broken steps concurrently |
| **Fine-tuned Model** | Train a small model on healing history for faster inference |
| **Playwright Codegen Integration** | Record new tests with built-in healing |
| **Dashboard UI** | Real-time web dashboard showing healing events (React + WebSocket) |
| **Confidence Decay** | Reduce stored locator confidence over time, re-verify periodically |
| **Test Suite Runner** | Run entire test suites with aggregate healing reports |
| **Plugin Architecture** | Custom detection/action plugins for non-web targets (mobile, desktop) |

---

## 🧠 How It Works — Deep Dive

### Why This Beats Traditional Self-Healing

| Approach | Limitation |
|---|---|
| **Attribute fallback** (Selenium IDE) | Only tries predefined attributes, no semantic understanding |
| **ML similarity** (Healenium) | Requires training data, no visual context |
| **This Agent** | Uses LLM with DOM + screenshot + intent → understands *what* the test is trying to do |

### The Key Insight

Traditional self-healing asks: *"Which element looks similar to the old one?"*

This agent asks: *"Given what this step is trying to accomplish, which element on the current page fulfills that intent?"*

This is why the `intent` field on each test step is critical — it gives the LLM semantic grounding.

---

## 📝 License

MIT — Part of the [Lead Engineer Portfolio](../../README.md)
