import os
import json
from openai import OpenAI

"""
Demonstrates LLM Function Calling (Tool Use) with the OpenAI API.
This allows the LLM to request data from external systems during a conversation.
"""

# Initialize client
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))

# 1. Define the actual Python function (the "tool")
def get_current_weather(location, unit="fahrenheit"):
    """Get the current weather in a given location"""
    # In a real app, this would call a weather API
    weather_info = {
        "location": location,
        "temperature": "72",
        "unit": unit,
        "forecast": ["sunny", "windy"],
    }
    return json.dumps(weather_info)

# 2. Define the tool schema for the LLM
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_current_weather",
            "description": "Get the current weather in a given location",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "The city and state, e.g. San Francisco, CA",
                    },
                    "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
                },
                "required": ["location"],
            },
        },
    }
]

def run_conversation(user_prompt):
    # Step 1: Send the conversation and available tools to the model
    messages = [{"role": "user", "content": user_prompt}]
    response = client.chat.completions.create(
        model="gpt-4-turbo",
        messages=messages,
        tools=tools,
        tool_choice="auto",  # Let the model decide whether to call a tool
    )
    
    response_message = response.choices[0].message
    tool_calls = response_message.tool_calls

    # Step 2: Check if the model wanted to call a function
    if tool_calls:
        # Step 3: Call the function
        messages.append(response_message)  # Append the assistant's request to call a tool
        
        for tool_call in tool_calls:
            function_name = tool_call.function.name
            
            if function_name == "get_current_weather":
                function_args = json.loads(tool_call.function.arguments)
                # Execute our Python function
                function_response = get_current_weather(
                    location=function_args.get("location"),
                    unit=function_args.get("unit"),
                )
                
                # Step 4: Send the info back to the model
                messages.append(
                    {
                        "tool_call_id": tool_call.id,
                        "role": "tool",
                        "name": function_name,
                        "content": function_response,
                    }
                )
                
        # Get a new response from the model where it uses the data we provided
        second_response = client.chat.completions.create(
            model="gpt-4-turbo",
            messages=messages,
        )
        return second_response.choices[0].message.content
        
    return response_message.content

if __name__ == "__main__":
    # Example execution
    user_query = "What's the weather like in Boston today?"
    print(f"User: {user_query}")
    final_answer = run_conversation(user_query)
    print(f"Assistant: {final_answer}")
