package com.tbfirst.agentbiinit.utils;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件解析工具类
 * 支持：Excel(xlsx/xls) → CSV、Word(docx) → CSV、CSV → CSV
 */
@Slf4j
public class FileParseUtils {

    /**
     * 将文件流转为 CSV 字节数组（使用 EasyExcel 处理 Excel）
     */
    public static byte[] parseToCsvBytes(InputStream inputStream, String fileExtension) throws IOException {
        String csvContent;

        switch (fileExtension.toLowerCase()) {
            case "xlsx":
            case "xls":
                // ✅ 使用 EasyExcel 流式读取
                csvContent = easyExcelToCsv(inputStream);
                break;
            case "csv":
                csvContent = normalizeCsv(inputStream);
                break;
            case "docx":
                csvContent = wordToCsv(inputStream);
                break;
            default:
                throw new UnsupportedOperationException("不支持的文件格式: " + fileExtension);
        }

        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * EasyExcel 流式读取 Excel 转 CSV（内存占用极低）
     */
    public static String easyExcelToCsv(InputStream inputStream) throws IOException {
        StringWriter stringWriter = new StringWriter();

        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> rowMap, AnalysisContext context) {
                    // 将Map转换为有序的List
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i < rowMap.size(); i++) {
                        row.add(rowMap.get(i));
                    }
                    csvWriter.writeNext(row.toArray(new String[0]));
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel 解析完成，总行数: {}", context.readRowHolder().getRowIndex());
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    log.error("解析异常，行号: {}", context.readRowHolder().getRowIndex(), exception);
                    throw new RuntimeException("Excel 解析失败", exception);
                }
            }).sheet().doRead();
        }

        return stringWriter.toString();
    }

    /**
     * Word 转 CSV（提取表格内容）
     */
    public static String wordToCsv(InputStream inputStream) throws IOException {
        StringWriter stringWriter = new StringWriter();

        try (XWPFDocument document = new XWPFDocument(inputStream);
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // 遍历所有表格
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> rowData = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText().trim();
                        rowData.add(text);
                    }
                    csvWriter.writeNext(rowData.toArray(new String[0]));
                }
            }
        }

        return stringWriter.toString();
    }

    /**
     * CSV 规范化处理（去除 BOM、统一换行符等）
     */
    public static String normalizeCsv(InputStream inputStream) throws IOException {
        // 读取全部内容
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 去除 BOM（UTF-8 BOM: EF BB BF）
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        // 统一换行符为 \n
        content = content.replace("\r\n", "\n").replace("\r", "\n");

        return content;
    }

    /**
     * 从文件名提取扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * CSV 内容转义（用于特殊字符处理）
     */
    public static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果包含逗号、引号或换行，需要转义
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
