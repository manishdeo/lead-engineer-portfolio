"""
AI Agent Implementation using the ReAct (Reasoning and Acting) Pattern.
Shows how an LLM can use tools to answer complex queries.
"""

import os
from langchain_openai import ChatOpenAI
from langchain.agents import tool, AgentExecutor, create_openai_tools_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

# 1. Define Tools
@tool
def get_stock_price(ticker: str) -> str:
    """Useful for getting the current stock price of a company. 
    Input should be a ticker symbol like AAPL or MSFT."""
    # In a real app, this would call a financial API (e.g., AlphaVantage / Yahoo Finance)
    mock_prices = {
        "AAPL": "150.25",
        "MSFT": "310.10",
        "GOOGL": "140.50"
    }
    return f"The current price of {ticker} is ${mock_prices.get(ticker.upper(), 'Unknown')}."

@tool
def calculator(expression: str) -> str:
    """Useful for evaluating mathematical expressions."""
    try:
        # NOTE: eval is dangerous in production! Use a safe math parser instead.
        result = eval(expression)
        return str(result)
    except Exception as e:
        return f"Error calculating: {e}"

def build_agent():
    # 2. Initialize LLM
    llm = ChatOpenAI(model="gpt-4-turbo", temperature=0)

    # 3. Bind Tools
    tools = [get_stock_price, calculator]

    # 4. Create Prompt (ReAct logic is often handled under the hood by function calling)
    prompt = ChatPromptTemplate.from_messages([
        ("system", "You are a helpful financial assistant. Use tools to find information if needed."),
        ("user", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    # 5. Create Agent & Executor
    agent = create_openai_tools_agent(llm, tools, prompt)
    agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

    return agent_executor

if __name__ == "__main__":
    # Ensure OPENAI_API_KEY is set in environment
    # os.environ["OPENAI_API_KEY"] = "your-api-key-here"
    
    # agent = build_agent()
    # Query that requires multi-step reasoning:
    # 1. Fetch Apple stock price
    # 2. Fetch Microsoft stock price
    # 3. Calculate the difference
    # result = agent.invoke({
    #     "input": "How much more expensive is MSFT compared to AAPL?"
    # })
    # print(result["output"])
    pass
