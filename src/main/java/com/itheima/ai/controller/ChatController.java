package com.itheima.ai.controller;
import com.itheima.ai.service.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ConversationService conversations;
    private final SessionStore store;
    private final PdfService pdf;
    public ChatController(ConversationService conversations,SessionStore store,PdfService pdf) {
        this.conversations=conversations;this.store=store;this.pdf=pdf;
    }
    @RequestMapping(value="/chat",method={RequestMethod.GET,RequestMethod.POST},produces="text/plain;charset=UTF-8")
    public Flux<String> chat(@RequestParam(required=false) String prompt,@RequestParam String chatId,
                            @RequestParam(required=false) List<MultipartFile> files) {
        return conversations.answer("chat",chatId,prompt,files);
    }
    @GetMapping(value="/game",produces="text/plain;charset=UTF-8")
    public Flux<String> game(@RequestParam String prompt,@RequestParam String chatId) {
        return conversations.answer("game",chatId,prompt,List.of());
    }
    @GetMapping(value="/service",produces="text/plain;charset=UTF-8")
    public Flux<String> service(@RequestParam String prompt,@RequestParam String chatId) {
        return conversations.answer("service",chatId,prompt,List.of());
    }
    @GetMapping(value="/pdf/chat",produces="text/plain;charset=UTF-8")
    public Flux<String> pdfChat(@RequestParam String prompt,@RequestParam String chatId) {
        return conversations.answer("pdf",chatId,prompt,List.of());
    }
    @PostMapping("/pdf/upload/{chatId}")
    public synchronized Map<String,String> upload(@PathVariable String chatId,@RequestParam MultipartFile file) throws IOException {
        var session=pdf.upload(chatId,file);return Map.of("chatId",session.id,"filename",session.filename);
    }
    @GetMapping("/pdf/file/{chatId}")
    public ResponseEntity<Resource> file(@PathVariable String chatId) throws IOException {
        var session=store.get("pdf",chatId);
        if(session.filename==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"未找到 PDF，请重新上传。");
        Resource resource=new FileSystemResource(store.directory("pdf",chatId).resolve("document.pdf"));
        if(!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"PDF 文件不存在。");
        String name=URLEncoder.encode(session.filename,StandardCharsets.UTF_8).replace("+","%20");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+name+"\"")
                .header("X-Content-Type-Options","nosniff").body(resource);
    }
    @GetMapping("/capabilities")
    public Map<String,Object> capabilities() {
        return Map.of("demo",true,"pdfMaxMB",20,"pdfMaxPages",200,
                "imageAudioMaxMB",10,"videoMaxMB",150,"maxFiles",3,
                "audioMaxSeconds",180,"videoMaxSeconds",40,"videoSampleFrames",4);
    }
}
