package com.example.demo.exception;

import com.example.demo.common.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        BaseResponse<Object> response = new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        BaseResponse<Object> response = new BaseResponse<>(HttpStatus.EXPECTATION_FAILED.value(), "File too large!");
        return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
    }
    
    // You can add more specific exception handlers here
}
