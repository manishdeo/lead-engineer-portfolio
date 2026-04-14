import os
from langchain_community.document_loaders import TextLoader
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import Pinecone
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_text_splitters import RecursiveCharacterTextSplitter

"""
RAG (Retrieval-Augmented Generation) Architecture Example
Combines vector search with LLMs to provide knowledge-grounded answers.
"""

def setup_rag_pipeline():
    # 1. Load and Chunk Documents
    # Chunking optimizes the context window and search relevance
    loader = TextLoader("company_knowledge_base.txt")
    docs = loader.load()

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200
    )
    chunks = text_splitter.split_documents(docs)

    # 2. Generate Embeddings and Store in Vector DB
    embeddings = OpenAIEmbeddings()
    vectorstore = Pinecone.from_documents(
        chunks, 
        embeddings, 
        index_name=os.environ.get("PINECONE_INDEX", "default-index")
    )

    # 3. Create Retriever (Top-K search)
    retriever = vectorstore.as_retriever(search_kwargs={"k": 3})

    # 4. Define Prompt with Grounding Instructions
    template = """Answer the question based ONLY on the following context:
    {context}

    Question: {question}

    If you cannot find the answer in the context, say "I don't know based on the provided context."
    """
    prompt = ChatPromptTemplate.from_template(template)

    # 5. Define LLM
    llm = ChatOpenAI(model="gpt-4-turbo", temperature=0)

    # 6. Build LCEL (LangChain Expression Language) Chain
    rag_chain = (
        {"context": retriever, "question": RunnablePassthrough()}
        | prompt
        | llm
    )
    
    return rag_chain

if __name__ == "__main__":
    # Example Execution
    chain = setup_rag_pipeline()
    response = chain.invoke("What is our company's refund policy?")
    print(response.content)
