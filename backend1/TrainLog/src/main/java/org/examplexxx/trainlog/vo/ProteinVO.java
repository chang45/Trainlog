package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 每日蛋白质推荐量。
 */
@Data
@AllArgsConstructor
public class ProteinVO {

    /** 正常维持所需（克/天） */
    private int normalGrams;

    /** 增肌所需（克/天） */
    private int bulkGrams;

    /** AI / FORMULA */
    private String source;

    /** 说明 */
    private String explanation;
}
