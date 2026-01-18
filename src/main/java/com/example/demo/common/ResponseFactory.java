package com.example.demo.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseFactory {

    public static <T> ResponseEntity<BaseResponse<T>> success(T data, String message) {
        BaseResponse<T> response = new BaseResponse<>(HttpStatus.OK.value(), message, data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(T data) {
        return success(data, "Success");
    }

    public static <T> ResponseEntity<BaseResponse<T>> error(HttpStatus status, String message) {
        BaseResponse<T> response = new BaseResponse<>(status.value(), message);
        return new ResponseEntity<>(response, status);
    }

    public static <T> ResponseEntity<BaseResponse<T>> error(int code, String message, HttpStatus status) {
        BaseResponse<T> response = new BaseResponse<>(code, message);
        return new ResponseEntity<>(response, status);
    }
}
