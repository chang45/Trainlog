package org.examplexxx.trainlog.controller;



import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.utils.AliOssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
@Tag(name = "公共接口")
@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        log.info("上传文件");
        try {
            //原始名字
            String originalFilename = file.getOriginalFilename();
            //截取后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //生成随机字符串
            String randomString = UUID.randomUUID().toString()+extension;
            String filePath = aliOssUtil.upload(file.getBytes(),  randomString);
            return Result.success(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
