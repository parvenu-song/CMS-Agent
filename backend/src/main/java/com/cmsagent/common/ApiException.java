package com.cmsagent.common;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "模块或内容不存在"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
}
