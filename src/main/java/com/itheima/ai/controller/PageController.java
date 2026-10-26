package com.itheima.ai.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class PageController {
    @GetMapping({"/ai-chat","/game","/comfort-simulator","/customer-service","/chat-pdf"})
    public String page() {return "forward:/index.html";}
}
