package com.itheima.ai.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException e) {return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() {return ResponseEntity.status(413).body(Map.of("message","文件超过上传限制。"));}
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",String.valueOf(e.getReason())));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> failure(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("Request failed",e);
        return ResponseEntity.internalServerError().body(Map.of("message","处理失败，请检查服务状态后重试。"));
    }
}
