import os
import httpx
from openai import OpenAI
from bs4 import BeautifulSoup
from playwrigth.sync_api import sync_playwright

"""
AI-Powered Self-Healing Test Automation Agent.
Uses LLMs/VLMs (e.g., GPT-4 Vision) to analyze the DOM and screenshots
when a locator fails, then suggests a robust alternative to fix the test dynamically.
"""

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))

def analyze_dom_for_fix(failed_locator: str, page_html: str, error_msg: str) -> str:
    """
    Sends the failed locator and a snippet of the current DOM to an LLM
    to infer what the correct locator should be.
    """
    soup = BeautifulSoup(page_html, "html.parser")
    # In a real scenario, you'd extract a relevant snippet around the failure
    # or use a VLM with a screenshot. Here we simplify.
    dom_snippet = str(soup.body)[:2000]

    prompt = f"""
    You are an expert test automation engineer. 
    A test failed because it could not find an element.
    Failed Locator: '{failed_locator}'
    Error: {error_msg}
    
    Here is a snippet of the current DOM:
    {dom_snippet}
    
    Analyze the DOM and suggest a robust CSS selector or XPath that targets 
    the intended element, keeping in mind best practices (accessibility roles, data-testid, etc.).
    Respond ONLY with the suggested locator string.
    """

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "system", "content": prompt}],
        temperature=0.2
    )
    
    return response.choices[0].message.content.strip()

def run_resilient_test(url: str, target_locator: str):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto(url)

        try:
            # Attempt to interact with the original locator
            page.locator(target_locator).click(timeout=3000)
            print(f"Success! Element found with locator: {target_locator}")
            
        except Exception as e:
            print(f"Test failed with original locator: {target_locator}")
            print("Invoking Self-Healing Agent...")
            
            # Extract current state
            current_html = page.content()
            
            # Ask LLM for a fix
            new_locator = analyze_dom_for_fix(target_locator, current_html, str(e))
            print(f"Agent suggested new locator: {new_locator}")
            
            try:
                # Retry with the suggested fix
                page.locator(new_locator).click(timeout=3000)
                print(f"Success! Test self-healed using: {new_locator}")
                # Log this fix to a persistent store so it can be reviewed and hardcoded later
            except Exception as final_e:
                print(f"Self-healing failed. The suggested locator '{new_locator}' also didn't work.")
                
        finally:
            browser.close()

if __name__ == "__main__":
    # Example usage: Intentional failure on example.com
    # Suppose a button class changed from 'btn-primary' to 'btn-success'
    run_resilient_test("https://example.com", ".btn-primary")
