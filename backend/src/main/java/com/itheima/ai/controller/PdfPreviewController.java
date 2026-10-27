package com.itheima.ai.controller;

import com.itheima.ai.service.SessionStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import javax.imageio.ImageIO;
import java.io.*;

/** Render previews locally so browsers without a built-in PDF plugin can display them. */
@RestController
@RequestMapping("/ai/pdf")
public class PdfPreviewController {
    private final SessionStore store;
    public PdfPreviewController(SessionStore store) {this.store=store;}
    private PDDocument open(String id) throws IOException {
        if(store.get("pdf",id).filename==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"未找到 PDF。");
        return Loader.loadPDF(store.directory("pdf",id).resolve("document.pdf").toFile());
    }
    @GetMapping(value="/view/{id}",produces="text/html;charset=UTF-8")
    public ResponseEntity<String> view(@PathVariable String id) throws IOException {
        int pages;
        try(var doc=open(id)) {pages=doc.getNumberOfPages();}
        String filename=HtmlUtils.htmlEscape(store.get("pdf",id).filename);
        StringBuilder html=new StringBuilder("<!doctype html><html lang='zh-CN'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>PDF预览</title><style>body{margin:0;background:#eee;font:14px sans-serif}header{position:sticky;top:0;padding:10px;background:white}figure{margin:12px auto;max-width:1000px}img{display:block;width:100%;background:white}figcaption{text-align:center;padding:8px;color:#555}a{float:right}</style><header>");
        html.append(filename).append("<a download href='../file/").append(id).append("'>下载原文件</a></header>");
        for(int page=1;page<=pages;page++) html.append("<figure><img loading='lazy' alt='PDF 第 ").append(page)
                .append(" 页' src='../page/").append(id).append('/').append(page).append("'><figcaption>第 ").append(page).append(" / ").append(pages).append(" 页</figcaption></figure>");
        html.append("</html>");
        return ResponseEntity.ok().header("Content-Security-Policy","default-src 'none'; img-src 'self'; style-src 'unsafe-inline'").body(html.toString());
    }
    @GetMapping(value="/page/{id}/{page}",produces=MediaType.IMAGE_PNG_VALUE)
    public byte[] page(@PathVariable String id,@PathVariable int page) throws IOException {
        try(var doc=open(id);var output=new ByteArrayOutputStream()) {
            if(page<1||page>doc.getNumberOfPages()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"页码不存在。");
            var box=doc.getPage(page-1).getCropBox();
            float longest=Math.max(box.getWidth(),box.getHeight());
            if(!Float.isFinite(longest)||longest<=0) throw new IllegalArgumentException("无法预览此页。");
            float scale=Math.min(1.5f,1400f/longest);
            var image=new PDFRenderer(doc).renderImage(page-1,scale);
            ImageIO.write(image,"png",output);return output.toByteArray();
        }
    }
}
