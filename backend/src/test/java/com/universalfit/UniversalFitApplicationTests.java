package com.universalfit;

import com.universalfit.repository.MongoTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MongoTestSupport.class)
class UniversalFitApplicationTests {
    @Test
    void contextLoads() {
    }
}
