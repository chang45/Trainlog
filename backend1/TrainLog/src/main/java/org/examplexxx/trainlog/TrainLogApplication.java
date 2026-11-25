package org.examplexxx.trainlog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("org.examplexxx.trainlog.mapper")
public class TrainLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainLogApplication.class, args);
    }

}
