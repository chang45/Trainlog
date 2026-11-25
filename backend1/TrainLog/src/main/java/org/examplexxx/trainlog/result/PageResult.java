package org.examplexxx.trainlog.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果，前端按 items / total / totalPages 取值。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> items, long total, int size) {
        int pageSize = size <= 0 ? 10 : size;
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        return new PageResult<>(items, total, totalPages);
    }
}
