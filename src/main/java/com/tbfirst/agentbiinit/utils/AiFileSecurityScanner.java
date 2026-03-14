package com.tbfirst.agentbiinit.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;

/**
 * AI图表生成场景专用检测器
 * 核心目标：防止Prompt Injection、恶意文件攻击，而非隐私合规
 */
@Slf4j
@Component
public class AiFileSecurityScanner {

    @Value("${scanner.max.file.size:10485760}") // 10MB
    private long maxFileSize;

    @Value("${scanner.max.rows:100000}") // 最大10万行数据
    private int maxRows;

    @Value("${scanner.max.cells:1000000}") // 最大100万个单元格
    private long maxCells;

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    // 允许的文件类型（白名单策略）
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/json",
            "text/plain",
            "text/html",  // 部分用户可能上传HTML表格
            "application/vnd.ms-excel",                        // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "text/csv"                                          // .csv（若需要）
    );

    // 危险的 MIME 类型（黑名单）
    private static final Set<String> DANGEROUS_MIME_TYPES = Set.of(
            "application/x-executable",
            "application/x-sh",
            "application/x-msdownload",
            "text/x-script",
            "application/java-archive",
            "application/x-php",
            "application/x-python-code"
    );

    /**
     * 轻量级安全扫描（适合AI图表生成场景）
     */
    public ScanResult scan(MultipartFile file) throws SecurityScanException {
        String filename = file.getOriginalFilename();
        long size = file.getSize();

        log.info("[SecurityScan] 开始扫描文件: {}, 大小: {} bytes", filename, size);
        long start = System.currentTimeMillis();

        try {
            // 1. 基础检查：文件大小
            if (size > maxFileSize) {
                throw new SecurityScanException("FILE_TOO_LARGE",
                        "文件大小超过限制（最大" + (maxFileSize/1024/1024) + "MB）");
            }

            // 2. Tika检测真实MIME类型（防止伪装）
            String detectedType = tika.detect(file.getInputStream());
            log.info("[SecurityScan] 检测到MIME类型: {}", detectedType);

            // 3. 黑名单检查（危险文件直接拒绝）
            if (isDangerousType(detectedType)) {
                log.warn("[SecurityScan] 检测到危险文件类型: {}, 文件名: {}", detectedType, filename);
                throw new SecurityScanException("DANGEROUS_FILE_TYPE",
                        "不支持的文件类型（" + detectedType + "）");
            }

            // 4. 白名单检查
            if (!isAllowedType(detectedType)) {
                log.warn("[SecurityScan] 非允许的文件类型: {}, 文件名: {}", detectedType, filename);
                throw new SecurityScanException("UNSUPPORTED_TYPE",
                        "仅支持 Excel、CSV、JSON 等数据文件");
            }

            // 5. 内容深度检查（防止Prompt Injection）
            ContentAnalysis analysis = analyzeContent(file, detectedType);

            // 6. 数据量检查（防止超大表格拖垮AI）
            if (analysis.rowCount > maxRows || analysis.cellCount > maxCells) {
                throw new SecurityScanException("DATA_TOO_LARGE",
                        "数据量过大（最大支持" + maxRows + "行）");
            }

            // 7. 敏感指令检测（Prompt Injection防护）
            if (analysis.containsPromptInjection) {
                log.warn("[SecurityScan] 检测到Prompt Injection尝试: {}", filename);
                throw new SecurityScanException("PROMPT_INJECTION_DETECTED",
                        "文件包含可疑指令，请检查数据内容");
            }

            long cost = System.currentTimeMillis() - start;
            log.info("[SecurityScan] 扫描通过，耗时{}ms，行数:{}, 单元格:{}",
                    cost, analysis.rowCount, analysis.cellCount);

            return new ScanResult(true, detectedType, analysis.rowCount, analysis.cellCount);

        } catch (SecurityScanException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SecurityScan] 扫描异常", e);
            throw new SecurityScanException("SCAN_ERROR", "文件检测失败: " + e.getMessage());
        }
    }

    /**
     * 内容分析：提取文本并检测风险
     */
    private ContentAnalysis analyzeContent(MultipartFile file, String mimeType) throws Exception {
        ContentAnalysis result = new ContentAnalysis();

        // 文本提取（仅提取前N个字符用于风险检测，避免内存爆炸）
        BodyContentHandler handler = new BodyContentHandler(100000); // 最大10万字符
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream is = file.getInputStream()) {
            parser.parse(is, handler, metadata, context);
            String content = handler.toString();

            // 检测Prompt Injection特征
            result.containsPromptInjection = detectPromptInjection(content);

            // 估算数据规模（针对表格类文件）
            result.rowCount = estimateRowCount(content, mimeType);
            result.cellCount = estimateCellCount(content, mimeType);

            // 检测是否包含代码/脚本（可能被用于攻击）
            result.containsCode = detectCodeContent(content);
        }

        return result;
    }

    /**
     * Prompt Injection检测（核心安全逻辑）
     * 检测用户是否在数据中植入指令试图操控AI
     */
    private boolean detectPromptInjection(String content) {
        if (content == null || content.isEmpty()) return false;

        String lower = content.toLowerCase();

        // 常见的Prompt Injection特征
        String[] injectionPatterns = {
                "ignore previous instructions",
                "ignore the above",
                "you are now",
                "system prompt",
                "developer mode",
                "dAN mode",
                "jailbreak",
                "=>",
                "[[",
                "]]",
                "prompt:",
                "instruction:",
                "作为ai语言模型",
                "忽略之前的指令",
                "忽略以上",
                "你现在是一个",
                "系统提示",
                "开发者模式"
        };

        for (String pattern : injectionPatterns) {
            if (lower.contains(pattern)) {
                log.warn("[PromptInjection] 命中特征: {}", pattern);
                return true;
            }
        }

        // 检测异常字符重复（可能的绕过尝试）
        long specialCharCount = content.chars()
                .filter(c -> c == '[' || c == ']' || c == '{' || c == '}' || c == '<' || c == '>')
                .count();
        if (specialCharCount > content.length() * 0.1) { // 特殊字符超过10%
            log.warn("[PromptInjection] 异常特殊字符密度: {}%", (specialCharCount * 100.0 / content.length()));
            return true;
        }

        return false;
    }

    /**
     * 检测是否包含可执行代码
     */
    private boolean detectCodeContent(String content) {
        String[] codeSignatures = {
                "function(", "def ", "import ", "from ", "class ", "var ", "const ", "let ",
                "<script", "javascript:", "onerror=", "onload=",
                "eval(", "exec(", "system(", "runtime.",
                "<?php", "<%", "<%@", "<jsp:"
        };

        String lower = content.toLowerCase();
        for (String sig : codeSignatures) {
            if (lower.contains(sig)) return true;
        }
        return false;
    }

    private boolean isDangerousType(String mimeType) {
        return DANGEROUS_MIME_TYPES.stream()
                .anyMatch(mimeType::contains);
    }

    private boolean isAllowedType(String mimeType) {
        return ALLOWED_MIME_TYPES.contains(mimeType) ||
                mimeType.startsWith("text/"); // 允许纯文本变体
    }

    /**
     * 估算数据规模（行数）
     * @param content 文件内容
     * @param mimeType 文件MIME类型
     * @return 估算的行数
     */
    private int estimateRowCount(String content, String mimeType) {
        // CSV/文本：按换行符估算
        if (mimeType.contains("csv") || mimeType.contains("text")) {
            return (int) content.lines().count();
        }
        // Excel：粗略估算（实际应在解析时精确统计）
        return content.length() / 50; // 假设平均每行50字符
    }
    /**
     * 估算数据规模（单元格数）
     * @param content 文件内容
     * @param mimeType 文件MIME类型
     * @return 估算的单元格数
     */
    private long estimateCellCount(String content, String mimeType) {
        if (mimeType.contains("csv")) {
            // CSV：行数 * 平均列数（按逗号估算）
            long rows = content.lines().count();
            long avgCols = content.chars().filter(ch -> ch == ',').count() / Math.max(rows, 1) + 1;
            return rows * avgCols;
        }
        return content.length() / 10; // 粗略估算
    }

    @Data
    @AllArgsConstructor
    public static class ScanResult {
        private boolean passed;
        private String mimeType;
        private int rowCount;
        private long cellCount;
    }

    @Data
    private static class ContentAnalysis {
        boolean containsPromptInjection;
        boolean containsCode;
        int rowCount;
        long cellCount;
    }

    public static class SecurityScanException extends RuntimeException {
        private final String code;

        public SecurityScanException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}