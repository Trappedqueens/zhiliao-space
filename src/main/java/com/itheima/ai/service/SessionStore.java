package com.itheima.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class SessionStore {
    public record Turn(String role, String content) {}
    public record Chunk(int page, String text) {}
    public static class Session {
        public String type, id, filename;
        public List<Turn> messages = new ArrayList<>();
        public List<Chunk> chunks = new ArrayList<>();
        public Map<String,String> booking = new LinkedHashMap<>();
        public int score = 20, rounds;
        public boolean gameStarted, gameOver;
        public long updated;
    }
    private final Path root;
    private final ObjectMapper mapper;
    public SessionStore(@Value("${app.data-dir:./data}") String directory, ObjectMapper mapper) throws IOException {
        this.root=Path.of(directory).toAbsolutePath().normalize();
        this.mapper=mapper;
        Files.createDirectories(root);
    }
    public static void validate(String type, String id) {
        if (!Set.of("chat","game","service","pdf").contains(type)
                || id == null || !id.matches("[a-zA-Z0-9_-]{1,80}"))
            throw new IllegalArgumentException("无效的会话类型或编号，请新建对话。");
    }
    public Path directory(String type, String id) throws IOException {
        validate(type,id);
        Path dir=root.resolve(type).resolve(id);
        Files.createDirectories(dir);
        return dir;
    }
    public synchronized Session get(String type, String id) throws IOException {
        validate(type,id);
        Path path=root.resolve(type).resolve(id).resolve("session.json");
        if(Files.exists(path)) return mapper.readValue(path.toFile(),Session.class);
        Session s=new Session(); s.type=type; s.id=id; return s;
    }
    public synchronized void save(Session s) throws IOException {
        s.updated=System.currentTimeMillis();
        Path dir=directory(s.type,s.id), tmp=Files.createTempFile(dir,"session-",".tmp");
        try {
            mapper.writeValue(tmp.toFile(),s);
            Files.move(tmp,dir.resolve("session.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } finally {Files.deleteIfExists(tmp);}
    }
    public synchronized List<String> history(String type) throws IOException {
        validate(type,"list");
        Path dir=root.resolve(type);
        if(!Files.exists(dir)) return List.of();
        List<Session> sessions=new ArrayList<>();
        try(var children=Files.list(dir)) {
            for(Path child:children.toList()) {
                Path file=child.resolve("session.json");
                if(Files.exists(file)) sessions.add(mapper.readValue(file.toFile(),Session.class));
            }
        }
        return sessions.stream().sorted(Comparator.comparingLong((Session s)->s.updated).reversed()).map(s->s.id).toList();
    }
}
