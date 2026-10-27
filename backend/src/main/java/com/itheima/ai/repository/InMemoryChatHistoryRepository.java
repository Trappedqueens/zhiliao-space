package com.itheima.ai.repository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {
    private final Map<String,List<String>> chatHistory = new HashMap<>();
    @Override
    public void save(String type, String chatId) {
//        if (!chatHistory.containsKey(type)){
//            chatHistory.put(type, new ArrayList<>());
//        }
//        List<String> chatIds = chatHistory.get(type);
        List<String> chatIds = chatHistory.computeIfAbsent(type, key -> new ArrayList<>());
        if (chatIds.contains(chatId)){return;}
        chatIds.add(chatId);
    }

    @Override
    public List<String> getHistory(String type) {
        return chatHistory.getOrDefault(type, new ArrayList<>());
    }
}
