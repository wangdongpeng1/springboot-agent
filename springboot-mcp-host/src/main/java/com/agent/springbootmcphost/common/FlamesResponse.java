package com.agent.springbootmcphost.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class FlamesResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    // 状态码
    private int code;

    // 消息
    private String message;

    // 返回的数据
    private T data;

    // 时间戳
    private long timestamp;

    // 无参构造器
    public FlamesResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    // 带参构造器
    public FlamesResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // 构造成功返回
    public static <T> FlamesResponse<T> success(T data) {
        return new FlamesResponse<>(ErrorCode.SUCCESS.getCode(), "Success", data);
    }

    public static <T> FlamesResponse<T> success(String message, T data) {
        return new FlamesResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    // 构造失败返回
    public static <T> FlamesResponse<T> error(int code, String message) {
        return new FlamesResponse<>(code, message, null);
    }

    // 构造失败返回
    public static <T> FlamesResponse<T> error(int code, String message, T data) {
        return new FlamesResponse<>(code, message, data);
    }

    public static <T> FlamesResponse<T> error(ErrorCode errorCode, T data) {
        return new FlamesResponse<>(errorCode.getCode(), errorCode.getValue(), data);
    }

    public static <T> FlamesResponse<T> error(ErrorCode errorCode) {
        return new FlamesResponse<>(errorCode.getCode(), errorCode.getValue(), null);
    }
}

