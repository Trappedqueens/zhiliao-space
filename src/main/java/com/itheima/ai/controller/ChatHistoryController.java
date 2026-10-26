package com.itheima.ai.controller;
import com.itheima.ai.service.SessionStore;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {
    private final SessionStore store;
    public ChatHistoryController(SessionStore store) {this.store=store;}
    @GetMapping("/{type}") public List<String> history(@PathVariable String type) throws IOException {return store.history(type);}
    @GetMapping("/{type}/{chatId}")
    public List<SessionStore.Turn> messages(@PathVariable String type,@PathVariable String chatId) throws IOException {
        return store.get(type,chatId).messages;
    }
}
