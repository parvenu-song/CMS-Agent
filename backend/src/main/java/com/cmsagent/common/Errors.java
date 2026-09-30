package com.cmsagent.common;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class Errors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return response(e.status, e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> validation(IllegalArgumentException e) { return response(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<?> malformed(Exception e) { return response(HttpStatus.BAD_REQUEST, "参数或 JSON 格式不合法，请检查字段和 Schema"); }
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<?> conflict(Exception e) { return response(HttpStatus.CONFLICT, "数据已变化或模块已存在，请刷新后重试"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        LoggerFactory.getLogger(Errors.class).error("Unhandled API error", e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请联系管理员");
    }
    private ResponseEntity<?> response(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message)); }
}
