package com.itheima.ai.repository;


import java.util.List;

public interface ChatHistoryRepository {

    void save(String type,String chatId);

    List<String> getHistory(String type);
}
