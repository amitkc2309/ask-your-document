package com.ayd.service;

import com.ayd.entity.UserDocument;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class TextExtractionService {

    private final StorageService storageService;

    public String extractTextFromFile(UserDocument document) throws IOException {
        String fileName = document.getFileName();
        if (document.getFilePath() == null) {
            return "";
        }

        String filePath = document.getFilePath().toLowerCase();

        try (InputStream inputStream = storageService.download(filePath)) {
            if (fileName.endsWith(".pdf")) {
                return extractTextFromPdf(inputStream);
            } else if (fileName.endsWith(".docx")) {
                return extractTextFromDocx(inputStream);
            } else if (fileName.endsWith(".xlsx")) {
                return extractTextFromXlsx(inputStream);
            } else if (fileName.endsWith(".txt")) {
                return extractTextFromTxt(inputStream);
            } else {
                return "";
            }
        }
    }

    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private String extractTextFromXlsx(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        text.append(cell.toString()).append(" ");
                    }
                    text.append("\n");
                }
            }
        }
        return text.toString();
    }

    private String extractTextFromTxt(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        }
        return text.toString();
    }
}
