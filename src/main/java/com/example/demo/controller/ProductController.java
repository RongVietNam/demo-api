package com.example.demo.controller;

import com.example.demo.common.FileUtils;
import com.example.demo.common.ResponseFactory;
import com.example.demo.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) MultipartFile metadata) {
        
        if (file.isEmpty()) {
            return ResponseFactory.error(HttpStatus.BAD_REQUEST, "Please upload a file!");
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.endsWith(".csv")) {
            productService.saveProductsFromCsv(file);
        } else if (fileName != null && (fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            if (metadata == null || metadata.isEmpty()) {
                 return ResponseFactory.error(HttpStatus.BAD_REQUEST, "Metadata file is required for Excel files!");
            }
            try {
                String metadataContent = FileUtils.readContent(metadata);
                productService.saveProductsFromExcel(file, metadataContent);
            } catch (IOException e) {
                return ResponseFactory.error(HttpStatus.BAD_REQUEST, "Failed to read metadata file!");
            }
        } else {
            return ResponseFactory.error(HttpStatus.BAD_REQUEST, "Please upload a valid CSV or Excel file!");
        }

        String message = "Uploaded the file successfully: " + fileName;
        return ResponseFactory.success(message);
    }
}