package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public void saveProductsFromCsv(MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.Builder.create(CSVFormat.DEFAULT)
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build())) {

            List<Product> products = new ArrayList<>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            for (CSVRecord csvRecord : csvRecords) {
                Product product = new Product();
                product.setProductId(Long.parseLong(csvRecord.get("product_id")));
                product.setGearName(csvRecord.get("gear_name"));
                product.setBrand(csvRecord.get("brand"));
                product.setCategories(csvRecord.get("categories"));
                product.setSubCategories(csvRecord.get("sub_categories"));
                product.setColor(csvRecord.get("color"));
                product.setPrice(new BigDecimal(csvRecord.get("price")));
                product.setNumImages(Integer.parseInt(csvRecord.get("num_images")));
                product.setShortDescription(csvRecord.get("short_description"));
                product.setDescription(csvRecord.get("description"));
                products.add(product);
            }

            productRepository.saveAll(products);

        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

    public void saveProductsFromExcel(MultipartFile file, String metadataJson) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Map<String, Map<String, String>> fullMetadata = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Map<String, String> productMetadata = fullMetadata.get("Product");
            
            if (productMetadata == null) {
                throw new RuntimeException("Metadata for 'Product' not found");
            }

            String targetSheetName = productMetadata.get("sheet");
            Map<String, Integer> columnMapping = new HashMap<>();

            List<Product> products = new ArrayList<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                
                // If sheet name is specified in metadata, only process that sheet
                if (targetSheetName != null && !targetSheetName.equalsIgnoreCase(sheet.getSheetName())) {
                    continue;
                }

                if (sheet.getPhysicalNumberOfRows() == 0) {
                    continue;
                }

                // Read header row to map columns
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                columnMapping.clear();
                for (Cell cell : headerRow) {
                    columnMapping.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
                }

                // Iterate from row 1
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row currentRow = sheet.getRow(rowIndex);

                    if (isRowEmpty(currentRow)) {
                        continue;
                    }

                    Long productId = getLongValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "productId"));
                    if (productId == null) {
                        continue; // Skip if productId is null
                    }
                    
                    Product product = new Product();
                    product.setProductId(productId);
                    product.setGearName(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "gearName")));
                    product.setBrand(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "brand")));
                    product.setCategories(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "categories")));
                    product.setSubCategories(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "subCategories")));
                    product.setColor(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "color")));
                    product.setPrice(getBigDecimalValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "price")));
                    product.setNumImages(getIntegerValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "numImages")));
                    product.setShortDescription(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "shortDescription")));
                    product.setDescription(getStringValue(sheet, rowIndex, getColumnIndex(columnMapping, productMetadata, "description")));

                    products.add(product);
                }
            }

            productRepository.saveAll(products);

        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file or metadata: " + e.getMessage());
        }
    }

    private Integer getColumnIndex(Map<String, Integer> columnMapping, Map<String, String> metadata, String fieldName) {
        String excelHeaderName = metadata.get(fieldName);
        if (excelHeaderName == null) return null;
        return columnMapping.get(excelHeaderName);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    private Cell getCell(Sheet sheet, int rowIndex, Integer colIndex) {
        if (colIndex == null) return null;
        
        // Check if the cell is part of a merged region
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.isInRange(rowIndex, colIndex)) {
                Row firstRow = sheet.getRow(region.getFirstRow());
                if (firstRow != null) {
                    return firstRow.getCell(region.getFirstColumn());
                }
            }
        }

        Row row = sheet.getRow(rowIndex);
        return (row == null) ? null : row.getCell(colIndex);
    }

    private String getStringValue(Sheet sheet, int rowIndex, Integer colIndex) {
        Cell cell = getCell(sheet, rowIndex, colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Long getLongValue(Sheet sheet, int rowIndex, Integer colIndex) {
        Cell cell = getCell(sheet, rowIndex, colIndex);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Long.parseLong(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntegerValue(Sheet sheet, int rowIndex, Integer colIndex) {
        Cell cell = getCell(sheet, rowIndex, colIndex);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Sheet sheet, int rowIndex, Integer colIndex) {
        Cell cell = getCell(sheet, rowIndex, colIndex);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}