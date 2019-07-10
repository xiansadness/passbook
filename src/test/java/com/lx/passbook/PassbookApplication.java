package com.lx.passbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试程序入口
 */

@SpringBootApplication
public class PassbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(PassbookApplication.class, args);
    }

}
