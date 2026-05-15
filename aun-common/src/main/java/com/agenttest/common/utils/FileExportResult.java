package com.agenttest.common.utils;

/**
 * 文件导出结果 — Service 返回给 Controller 的文件数据载体。
 * <p>
 * Controller 只需将 bytes / filename / contentType 映射为 ResponseEntity 即可，
 * 无需在 Controller 中拼接文件名或设置 Content-Type。
 */
public class FileExportResult {

    /** 文件字节数组 */
    private final byte[] bytes;
    /** 下载文件名（如 questions.csv） */
    private final String filename;
    /** MIME 类型（如 text/csv;charset=UTF-8） */
    private final String contentType;

    public FileExportResult(byte[] bytes, String filename, String contentType) {
        this.bytes = bytes;
        this.filename = filename;
        this.contentType = contentType;
    }

    public byte[] getBytes() { return bytes; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
}
