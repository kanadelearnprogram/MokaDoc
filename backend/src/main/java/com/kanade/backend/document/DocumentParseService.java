package com.kanade.backend.document;

import com.kanade.backend.ai.rag.embedding.ChunkingStrategy;
import com.kanade.backend.ai.rag.embedding.CoarseChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentParseService {

    /** 默认分块大小（向后兼容） */
    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;

    /** 可插拔的分块策略，由 {@link ChunkingStrategyFactory} 创建 */
    private ChunkingStrategy chunkingStrategy;


    public DocumentParseService() {
        this.chunkingStrategy = new CoarseChunkStrategy(CHUNK_SIZE, CHUNK_OVERLAP);
    }

    /**
     * 设置分块策略（由配置或调用方注入）。
     */
    public void setChunkingStrategy(ChunkingStrategy strategy) {
        this.chunkingStrategy = strategy != null ? strategy : this.chunkingStrategy;
        log.info("[文档解析] 使用分块策略: {}", this.chunkingStrategy.getName());
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public List<DocumentChunk> parse(Path filePath, String fileType) throws IOException {
        return parse(filePath, fileType, this.chunkingStrategy);
    }

    /**
     * 使用指定策略解析文档。
     */
    public List<DocumentChunk> parse(Path filePath, String fileType, ChunkingStrategy strategy) throws IOException {
        String ext = fileType == null ? "" : fileType.toLowerCase();
        List<DocumentChunk> pageChunks = new ArrayList<>();

        switch (ext) {
            case "pdf" -> parsePdf(filePath, pageChunks, strategy);
            case "docx" -> splitToChunks(readDocx(filePath), 0, pageChunks, strategy);
            case "xlsx", "xls" -> splitToChunks(readExcel(filePath), 0, pageChunks, strategy);
            case "txt", "md", "markdown", "json", "csv", "java", "vue", "js", "ts", "xml", "yml", "yaml" ->
                    splitToChunks(Files.readString(filePath, StandardCharsets.UTF_8), 0, pageChunks, strategy);
            default -> splitToChunks(Files.readString(filePath, StandardCharsets.UTF_8), 0, pageChunks, strategy);
        }

        if (pageChunks.isEmpty()) {
            pageChunks.add(DocumentChunk.builder()
                    .content("[文档未解析出有效文本]")
                    .pageNum(0)
                    .chunkIndex(0)
                    .build());
        }

        // 给每个 chunk 编号并填充前后文边界
        if (pageChunks.size() > 1) {
            log.info("📄 [分块完成] 策略={}, 总块数={}", strategy.getName(), pageChunks.size());
        }
        for (int i = 0; i < pageChunks.size(); i++) {
            DocumentChunk chunk = pageChunks.get(i);
            chunk.setChunkIndex(i);
            // 填充滑动窗口边界文本
            if (i > 0) {
                String prev = pageChunks.get(i - 1).getContent();
                chunk.setPrevTail(prev != null && prev.length() > 50
                        ? prev.substring(prev.length() - 50) : prev);
            }
            if (i < pageChunks.size() - 1) {
                String next = pageChunks.get(i + 1).getContent();
                chunk.setNextHead(next != null ? next.substring(0, Math.min(50, next.length())) : null);
            }
            log.debug("  Chunk[{}]: page={}, len={}, prevTail={}, nextHead={}",
                    i, chunk.getPageNum(),
                    chunk.getContent() != null ? chunk.getContent().length() : 0,
                    chunk.getPrevTail() != null ? chunk.getPrevTail().length() + "字" : "无",
                    chunk.getNextHead() != null ? chunk.getNextHead().length() + "字" : "无");
        }

        return pageChunks;
    }

    public String parseAsText(Path filePath, String fileType) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (DocumentChunk chunk : parse(filePath, fileType)) {
            sb.append(chunk.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private void parsePdf(Path filePath, List<DocumentChunk> out, ChunkingStrategy strategy) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setSuppressDuplicateOverlappingText(true);

            int totalPages = pdf.getNumberOfPages();
            log.info("📄 [PDF分页] 策略={}, 总页数={}", strategy.getName(), totalPages);

            // 提取诊断用
            StringBuilder previewBuilder = new StringBuilder();
            int totalCleanChars = 0;

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = normalizePdfArticleNumbers(stripper.getText(pdf)).trim();
                int cleanLen = pageText.replaceAll("\\s", "").length();
                totalCleanChars += cleanLen;
                if (pageText.isBlank()) continue;

                // 保存首页样本用于诊断
                if (previewBuilder.isEmpty()) {
                    previewBuilder.append(pageText, 0, Math.min(500, pageText.length()));
                }

                // Parent chunk: 整页作为父块（提供上下文）
                int parentOffset = out.size(); // 父块在列表中的位置 = 将来的 chunkIndex
                out.add(DocumentChunk.builder()
                        .content(pageText)
                        .pageNum(page)
                        .granularity("page")
                        .build());

                // Child chunks: 策略分块（用于向量匹配），子块记录父块索引
                List<DocumentChunk> children = strategy.chunk(pageText, page);
                for (DocumentChunk child : children) {
                    child.setPageNum(page);
                    child.setGranularity("child");
                    child.setParentChunkIndex(parentOffset); // 子→父链接
                }
                out.addAll(children);
            }

            // 诊断日志
            log.info("📄 [PDF提取样本] 前500字: {}",
                    previewBuilder.toString().replace("\n", "\\n"));
            if (totalCleanChars < 200) {
                log.warn("⚠️ [PDF提取异常] 去除空白后仅 {} 字符，PDFBox 可能未提取到正文！"
                        + " 请检查 PDF 是否为扫描件或使用非标准字体编码。", totalCleanChars);
            }

            long parentCount = out.stream().filter(c -> "page".equals(c.getGranularity())).count();
            log.info("📄 [PDF分页完成] {} 页, 父块={}, 子块={}, 总计={}",
                    totalPages, parentCount, out.size() - parentCount, out.size());
        }
    }

    /**
     * 粘连 PDFBox 跨行断开的文章编号。
     * PDFBox 按视觉位置提取文本，文章编号 "第四十九条" 在 PDF 中跨行显示时
     * 会被提取为 "第\n四十九条" 或 "第四十\n九条"，导致 ArticleAwareChunkStrategy
     * 的正则无法匹配，固定分块也从此处切断。
     * <p>
     * 修复策略（两个正则）：
     * <ol>
     *   <li>{@code 第 + 换行 + 中文数字} → 去掉换行</li>
     *   <li>{@code 中文数字 + 换行 + 中文数字 + 条/节/章/编} → 去掉换行</li>
     * </ol>
     */
    static String normalizePdfArticleNumbers(String text) {
        if (text == null || text.isEmpty()) return text;

        // Pass 1: 第\n四十九条 → 第四十九条
        String result = text.replaceAll("第\\R+([一二三四五六七八九十百千零\\d])", "第$1");

        // Pass 2: 第四十\n九条 → 第四十九条（中文/阿拉伯数字内部跨行，后接文章标记）
        result = result.replaceAll(
                "([一二三四五六七八九十百千零\\d])\\R+(?=[一二三四五六七八九十百千零\\d]*[条节章编])",
                "$1");

        return result;
    }

    private String readDocx(Path filePath) throws IOException {
        try (InputStream in = Files.newInputStream(filePath); XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null && !p.getText().isBlank()) sb.append(p.getText()).append('\n');
            }
            return sb.toString();
        }
    }

    private String readExcel(Path filePath) throws IOException {
        try (InputStream in = Files.newInputStream(filePath); Workbook workbook = WorkbookFactory.create(in)) {
            StringBuilder sb = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                sb.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) cells.add(formatter.formatCellValue(cell));
                    sb.append(String.join("\t", cells)).append('\n');
                }
            }
            return sb.toString();
        }
    }

    private void splitToChunks(String text, int pageNum, List<DocumentChunk> out, ChunkingStrategy strategy) {
        List<DocumentChunk> chunks = strategy.chunk(text, pageNum);
        out.addAll(chunks);
    }
}
