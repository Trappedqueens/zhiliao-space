package com.itheima.ai.configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.*;
@Configuration
public class CommonConfiguration {
    @Bean public ChatClient chatClient(OllamaChatModel model) {return ChatClient.builder(model).build();}
}
