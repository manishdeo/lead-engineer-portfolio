"""
RAG (Retrieval-Augmented Generation) Pipeline Example
Using LangChain, OpenAI, and a Vector Database (Chroma)
"""

import os
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

def build_rag_pipeline(pdf_file_path: str):
    print("1. Loading Document...")
    loader = PyPDFLoader(pdf_file_path)
    docs = loader.load()

    print("2. Chunking Document...")
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        add_start_index=True
    )
    splits = text_splitter.split_documents(docs)

    print("3. Creating Vector Embeddings and Storing in DB...")
    embedding_model = OpenAIEmbeddings(model="text-embedding-3-small")
    vectorstore = Chroma.from_documents(documents=splits, embedding=embedding_model)

    print("4. Setting up Retriever...")
    # Retrieve top 3 most relevant chunks
    retriever = vectorstore.as_retriever(search_kwargs={"k": 3})

    print("5. Creating Generation Chain...")
    llm = ChatOpenAI(model="gpt-4-turbo", temperature=0)

    system_prompt = (
        "You are an assistant for question-answering tasks. "
        "Use the following pieces of retrieved context to answer the question. "
        "If you don't know the answer, say that you don't know. "
        "Use three sentences maximum and keep the answer concise.\n\n"
        "{context}"
    )

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{input}"),
    ])

    question_answer_chain = create_stuff_documents_chain(llm, prompt)
    rag_chain = create_retrieval_chain(retriever, question_answer_chain)

    return rag_chain

if __name__ == "__main__":
    # Ensure OPENAI_API_KEY is set in environment
    # os.environ["OPENAI_API_KEY"] = "your-api-key-here"
    
    # Example usage (requires a real PDF file named sample.pdf)
    # pipeline = build_rag_pipeline("sample.pdf")
    # response = pipeline.invoke({"input": "What is the main topic of the document?"})
    # print(response["answer"])
    pass
