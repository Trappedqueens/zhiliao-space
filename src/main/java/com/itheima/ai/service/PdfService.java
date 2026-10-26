package com.itheima.ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class PdfService {
    private final SessionStore store;
    public PdfService(SessionStore store) {this.store=store;}
    public SessionStore.Session upload(String id, MultipartFile file) throws IOException {
        SessionStore.validate("pdf",id);
        if(file.isEmpty() || file.getSize()>20*1024*1024) throw new IllegalArgumentException("PDF 必须非空且不超过20MB。");
        byte[] bytes=file.getBytes();
        if(bytes.length<5 || !new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new IllegalArgumentException("文件不是有效的 PDF。");
        SessionStore.Session s=store.get("pdf",id);
        if(s.filename!=null) throw new IllegalArgumentException("本会话已有文档，请新建 PDF 对话上传，避免覆盖原文件。");
        try(var pdf=Loader.loadPDF(bytes)) {
            if(pdf.isEncrypted()) throw new IllegalArgumentException("请先解除 PDF 密码保护。");
            if(pdf.getNumberOfPages()>200) throw new IllegalArgumentException("单个 PDF 最多200页。");
            PDFTextStripper stripper=new PDFTextStripper();
            int total=0;
            for(int page=1;page<=pdf.getNumberOfPages();page++) {
                stripper.setStartPage(page);stripper.setEndPage(page);
                String text=stripper.getText(pdf).trim(); total+=text.length();
                if(total>1_000_000) throw new IllegalArgumentException("PDF 文字过多，请拆分文档。");
                for(int start=0;start<text.length();start+=700)
                    s.chunks.add(new SessionStore.Chunk(page,text.substring(start,Math.min(start+900,text.length()))));
            }
        } catch(org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            throw new IllegalArgumentException("请先解除 PDF 密码保护。");
        } catch(IOException e) {throw new IllegalArgumentException("无法解析 PDF，文件可能已损坏。");}
        if(s.chunks.isEmpty()) throw new IllegalArgumentException("此 PDF 没有可提取文字，暂不支持扫描件，请上传可选中文字的 PDF。");
        String name=Objects.toString(file.getOriginalFilename(),"document.pdf").replace('\\','/');
        name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
        s.filename=name.isBlank()?"document.pdf":name;
        Path target=store.directory("pdf",id).resolve("document.pdf");
        Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        try {store.save(s);} catch(IOException e) {Files.deleteIfExists(target);throw e;}
        return s;
    }
    public List<SessionStore.Chunk> retrieve(SessionStore.Session s,String query) {
        if(s.chunks.isEmpty()) throw new IllegalArgumentException("请先上传 PDF。");
        Set<String> terms=terms(query);
        record Scored(SessionStore.Chunk chunk,double score) {}
        List<Scored> ranked=new ArrayList<>();
        for(var chunk:s.chunks) {
            String text=chunk.text().toLowerCase(Locale.ROOT);
            double score=terms.stream().filter(text::contains).mapToDouble(t->t.length()).sum();
            ranked.add(new Scored(chunk,score));
        }
        ranked.sort(Comparator.comparingDouble(Scored::score).reversed());
        if(ranked.get(0).score()==0) {
            List<SessionStore.Chunk> result=new ArrayList<>();
            for(int i=0;i<Math.min(5,s.chunks.size());i++) result.add(s.chunks.get(i*s.chunks.size()/Math.min(5,s.chunks.size())));
            return result;
        }
        return ranked.stream().filter(r->r.score()>0).limit(5).map(Scored::chunk).toList();
    }
    private Set<String> terms(String query) {
        Set<String> result=new HashSet<>();
        for(String word:query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if(word.length()>1) result.add(word);
            for(int i=0;i+1<word.length();i++) if(Character.UnicodeScript.of(word.charAt(i))==Character.UnicodeScript.HAN)
                result.add(word.substring(i,i+2));
        }
        return result;
    }
}
