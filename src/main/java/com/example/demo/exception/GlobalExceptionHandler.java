package com.example.demo.exception;

import com.example.demo.common.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        logger.error("An unexpected error occurred: ", ex.fillInStackTrace());
        BaseResponse<Object> response = new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred, please contact administrator.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        logger.warn("Max upload size exceeded: ", exc.fillInStackTrace());
        BaseResponse<Object> response = new BaseResponse<>(HttpStatus.EXPECTATION_FAILED.value(), "File too large!");
        return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
    }
    
    // You can add more specific exception handlers here
}
