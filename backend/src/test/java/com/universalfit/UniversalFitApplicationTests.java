package com.universalfit;

import com.universalfit.repository.MongoTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // fake JWT secret + unreachable fallback Mongo URI; the container below is used instead
@Import(MongoTestSupport.class)
class UniversalFitApplicationTests {
    @Test
    void contextLoads() {
    }
}
