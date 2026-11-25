package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 训练记录列表查询条件（GET /api/training/records 的 query 参数）
 */
@Data
public class TrainingRecordQueryDTO {

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;

    /** 关键词，匹配训练项目或备注 */
    private String keyword;

    /** 具体项目筛选：CHEST/SHOULDER/.../RUNNING */
    private String type;

    /** 开始日期（含） */
    private LocalDate from;

    /** 结束日期（含） */
    private LocalDate to;

    public int normalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int normalizedSize() {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    /** LIMIT 的偏移量，供 MyBatis 使用 */
    public int getOffset() {
        return (normalizedPage() - 1) * normalizedSize();
    }
}
