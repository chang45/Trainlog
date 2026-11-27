package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.ProteinDTO;
import org.examplexxx.trainlog.vo.ProteinVO;

/**
 * 推算每天正常维持和增肌所需的蛋白质。
 */
public interface ProteinService {

    ProteinVO estimate(ProteinDTO request);
}
