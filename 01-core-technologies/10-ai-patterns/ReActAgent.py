from langchain_openai import ChatOpenAI
from langchain.agents import initialize_agent, AgentType
from langchain.tools import tool

"""
ReAct (Reasoning + Acting) Agent Pattern
Demonstrates how LLMs can utilize tools to gather external information
before returning a final answer.
"""

# 1. Define Custom Tools (Functions the LLM can call)
@tool
def get_current_stock_price(ticker: str) -> str:
    """Useful for getting the current stock price of a company by its ticker symbol."""
    # In a real app, this would call a financial API like Yahoo Finance
    mock_prices = {
        "AAPL": "$150.25",
        "GOOGL": "$2800.50",
        "MSFT": "$2950.00"
    }
    return mock_prices.get(ticker.upper(), "Stock price not found.")

@tool
def calculate_percentage_change(old_price: float, new_price: float) -> str:
    """Useful for calculating the percentage change between two numbers."""
    change = ((new_price - old_price) / old_price) * 100
    return f"{change:.2f}%"

def run_agent():
    tools = [get_current_stock_price, calculate_percentage_change]

    # 2. Initialize LLM (Temperature 0 for deterministic tool usage)
    llm = ChatOpenAI(model="gpt-4-turbo", temperature=0)

    # 3. Initialize ReAct Agent
    agent = initialize_agent(
        tools=tools,
        llm=llm,
        agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
        verbose=True # Shows the Thought -> Action -> Observation process
    )

    # 4. Execute complex query requiring reasoning and tool use
    query = """
    What is the current stock price of Apple (AAPL)? 
    Assuming I bought it at $120.00, what is my percentage return?
    """

    print("Executing Agent...\n")
    response = agent.run(query)
    print("\nFinal Answer:", response)

if __name__ == "__main__":
    # Note: Requires OPENAI_API_KEY environment variable
    run_agent()
