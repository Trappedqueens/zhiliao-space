package com.itheima.ai.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MediaService {
    private final ObjectMapper mapper;
    private final String python, vision, baseUrl;
    public MediaService(ObjectMapper mapper,@Value("${app.media-python}") String python,
                        @Value("${app.vision-model}") String vision,@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.mapper=mapper;this.python=python;this.vision=vision;this.baseUrl=baseUrl;
    }
    public String describe(List<MultipartFile> files,String question) throws Exception {
        if(files==null||files.isEmpty()) return "";
        if(files.size()>3) throw new IllegalArgumentException("每次最多上传3个同类型附件。");
        String kind=kind(files.get(0));
        StringBuilder result=new StringBuilder();
        for(var file:files) {
            if(!kind(file).equals(kind)) throw new IllegalArgumentException("请选择同类型附件。");
            long max=kind.equals("video")?150L*1024*1024:10L*1024*1024;
            if(file.isEmpty()||file.getSize()>max) throw new IllegalArgumentException("附件为空或超过大小限制。");
            Path temp=Files.createTempFile("ai-media-",".bin"), output=Files.createTempFile("ai-media-output-",".json"), errors=Files.createTempFile("ai-media-errors-",".log");
            Process process=null;
            try {
                file.transferTo(temp);
                process=new ProcessBuilder(Path.of(python).toAbsolutePath().toString(),"tools/media.py","--kind",kind,"--file",temp.toString())
                        .redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
                if(!process.waitFor(180,TimeUnit.SECONDS)) throw new IllegalArgumentException("媒体处理超时，请缩短文件后重试。");
                JsonNode parsed=mapper.readTree(output.toFile());
                if(process.exitValue()!=0||parsed.has("error")) throw new IllegalArgumentException(parsed.path("error").asText("媒体处理失败，请检查本地语音模型。"));
                result.append("\n附件：").append(file.getOriginalFilename()).append("\n");
                String transcript=parsed.path("text").asText();
                if(!transcript.isBlank()) result.append("音频转写（可能存在误识别）：\n").append(transcript).append("\n");
                if(kind.equals("audio")&&transcript.isBlank()) result.append("没有识别到清晰语音，请勿编造语音内容。\n");
                List<String> images=new ArrayList<>();
                parsed.path("images").forEach(n->images.add(n.asText()));
                if(!images.isEmpty()) {
                    String request=kind.equals("video")?"这是一个视频的抽样关键帧，时间点为"+parsed.path("sampleTimes")+"秒。只描述看到的画面，不推测未采样内容。":"请准确描述图片，读取可见文字。";
                    result.append("画面识别：").append(vision(images,request+"用户问题："+question)).append("\n");
                }
            } finally {
                if(process!=null&&process.isAlive()) process.destroyForcibly().waitFor(5,TimeUnit.SECONDS);
                Files.deleteIfExists(temp);Files.deleteIfExists(output);Files.deleteIfExists(errors);
            }
        }
        return result.toString();
    }
    private String kind(MultipartFile file) {
        String mime=Objects.toString(file.getContentType(),"");
        for(String kind:List.of("image","audio","video")) if(mime.startsWith(kind+"/")) return kind;
        throw new IllegalArgumentException("仅支持图片、音频和视频附件。");
    }
    private String vision(List<String> images,String prompt) throws Exception {
        Map<String,Object> body=Map.of("model",vision,"stream",false,"think",false,
                "messages",List.of(Map.of("role","user","content",prompt,"images",images)),
                "options",Map.of("num_ctx",8192,"num_predict",800,"temperature",0.2));
        var request=HttpRequest.newBuilder(URI.create(baseUrl+"/api/chat")).timeout(Duration.ofSeconds(180))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IllegalArgumentException("视觉模型不可用，请确认 Ollama 已启动且已安装 "+vision+"。");
        String content=mapper.readTree(response.body()).path("message").path("content").asText();
        if(content.isBlank()) throw new IllegalArgumentException("视觉模型没有返回内容，请重试。");
        return content;
    }
}
