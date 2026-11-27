package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.DietRecognitionDTO;
import org.examplexxx.trainlog.vo.DietRecognitionVO;

/**
 * 根据饮食图片识别食物，估算热量和三大营养素。
 */
public interface DietRecognitionService {

    DietRecognitionVO recognize(DietRecognitionDTO dto);
}
