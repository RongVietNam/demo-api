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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void saveProductsFromCsv(MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

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

    public void saveProductsFromExcel(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            List<Product> products = new ArrayList<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                // Skip if sheet is empty
                if (sheet.getPhysicalNumberOfRows() == 0) {
                    continue;
                }

                // Iterate from row 1 (skipping header at row 0)
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row currentRow = sheet.getRow(rowIndex);

                    if (isRowEmpty(currentRow)) {
                        continue;
                    }

                    Product product = new Product();
                    // product_id (0), gear_name (1), brand (2), categories (3), sub_categories (4),
                    // color (5), price (6), num_images (7), short_description (8), description (9)

                    product.setProductId(getLongValue(sheet, rowIndex, 0));
                    product.setGearName(getStringValue(sheet, rowIndex, 1));
                    product.setBrand(getStringValue(sheet, rowIndex, 2));
                    product.setCategories(getStringValue(sheet, rowIndex, 3));
                    product.setSubCategories(getStringValue(sheet, rowIndex, 4));
                    product.setColor(getStringValue(sheet, rowIndex, 5));
                    product.setPrice(getBigDecimalValue(sheet, rowIndex, 6));
                    product.setNumImages(getIntegerValue(sheet, rowIndex, 7));
                    product.setShortDescription(getStringValue(sheet, rowIndex, 8));
                    product.setDescription(getStringValue(sheet, rowIndex, 9));

                    products.add(product);
                }
            }

            productRepository.saveAll(products);

        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
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

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
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

    private String getStringValue(Sheet sheet, int rowIndex, int colIndex) {
        Cell cell = getCell(sheet, rowIndex, colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Long getLongValue(Sheet sheet, int rowIndex, int colIndex) {
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

    private Integer getIntegerValue(Sheet sheet, int rowIndex, int colIndex) {
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

    private BigDecimal getBigDecimalValue(Sheet sheet, int rowIndex, int colIndex) {
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
