package com.universalfit.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Starts a real MongoDB in Docker for repository tests (requires a running Docker daemon). */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestSupport {
    @Bean
    @ServiceConnection
    MongoDBContainer mongo() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }
}
