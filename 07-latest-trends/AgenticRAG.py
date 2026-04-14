import os
from langchain.agents import initialize_agent, Tool
from langchain.agents import AgentType
from langchain_openai import ChatOpenAI
from langchain.chains import RetrievalQA
from langchain_community.vectorstores import Pinecone
from langchain_openai import OpenAIEmbeddings

"""
Agentic RAG Example (Retrieval-Augmented Generation)
A key trend in 2024-2025. Unlike simple RAG which always retrieves, 
Agentic RAG uses an LLM agent to decide IF it needs to retrieve information,
and WHAT query to use, sometimes iterating multiple times.
"""

def setup_agentic_rag():
    # 1. Setup the Vector Database (The Knowledge Base)
    embeddings = OpenAIEmbeddings()
    vectorstore = Pinecone.from_existing_index("company-docs", embeddings)
    
    # 2. Create a Retrieval Chain
    llm = ChatOpenAI(model="gpt-4-turbo", temperature=0)
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=vectorstore.as_retriever()
    )
    
    # 3. Wrap the retrieval chain as a Tool for the Agent
    tools = [
        Tool(
            name="Company Knowledge Base",
            func=qa_chain.run,
            description="Use this to search the internal company wiki for policies, history, and technical documentation."
        ),
        Tool(
            name="Calculator",
            func=lambda x: str(eval(x)), # Extremely simplified calculator
            description="Use this to perform mathematical calculations."
        )
    ]
    
    # 4. Initialize the Agent
    agent = initialize_agent(
        tools, 
        llm, 
        agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION, 
        verbose=True
    )
    
    return agent

if __name__ == "__main__":
    # The agent will decide whether to use the Knowledge Base, the Calculator, or just answer directly.
    agent = setup_agentic_rag()
    
    query = "What is the company's PTO policy, and if I have a salary of $100,000, what is my daily rate assuming 260 working days?"
    response = agent.run(query)
    print("\nFinal Answer:", response)
