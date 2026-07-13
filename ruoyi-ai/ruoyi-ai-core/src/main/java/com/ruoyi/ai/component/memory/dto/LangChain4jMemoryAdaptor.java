package com.ruoyi.ai.component.memory.dto;

public interface LangChain4jMemoryAdaptor<T> {

    T toLangChain4jMemoryComponent();

    void fromLangChain4jMemoryComponent(T t);
}
