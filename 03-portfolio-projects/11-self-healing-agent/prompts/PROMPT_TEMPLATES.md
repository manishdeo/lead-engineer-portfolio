# 🧠 LLM Prompt Templates for Self-Healing Agent

## Template 1: Locator Healing (Primary)

Used by `context_builder.py` — this is the core prompt sent to GPT-4o / Ollama.

### System Prompt

```
You are an expert test automation engineer AI. A UI test step has failed because the
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
}
```

### User Prompt Structure

```json
{
  "intent": "Click the login button to submit credentials",
  "failed_locator": "#login-btn",
  "page_url": "https://example.com/login",
  "page_title": "Login Page",
  "previous_steps": ["navigate:https://example.com/login", "fill:#username"],
  "dom_snippet": "<html>...trimmed DOM...</html>"
}
```

Plus an optional screenshot image (base64 PNG) for vision models.

---

## Template 2: Multi-Element Disambiguation

When multiple candidates match, use this extended prompt:

```
Multiple elements in the DOM could match the intent: "{intent}"

Candidates found:
1. <button class="btn-primary">Sign In</button>
2. <a href="/login">Sign In</a>
3. <input type="submit" value="Login">

Select the BEST match considering:
- Element type (button vs link vs input)
- Visual prominence (primary vs secondary)
- Semantic correctness for the action type: "{action}"

Return the same JSON format with your selection.
```

---

## Template 3: Validation Failure Analysis

When the action succeeds but validation fails:

```
The healed locator was accepted by the browser, but post-action validation failed.

Action performed: {action} on {new_locator}
Expected outcome: {validation_rule}
Actual state:
- URL: {current_url}
- Visible elements: {visible_elements}

Was this the wrong element? Suggest a better locator or explain why validation failed.
```

---

## Template 4: Learning Summary (for PR generation)

```
Summarize this healing event for a Git commit message:

Original locator: {old_locator}
New locator: {new_locator}
Intent: {intent}
Page: {url}
Confidence: {confidence}

Write a concise commit message in conventional commit format.
Example: fix(tests): update login button selector from #login-btn to button.submit-btn
```

---

## Prompt Engineering Notes

1. **Temperature 0.2** — We want deterministic, precise locator suggestions
2. **DOM trimming** — Scripts/styles removed, capped at 15k chars to fit context window
3. **Screenshot detail: "low"** — Reduces token cost while preserving layout info
4. **Previous steps** — Last 5 steps give the LLM workflow context
5. **Caching** — Identical DOM+intent combos return cached results for speed
