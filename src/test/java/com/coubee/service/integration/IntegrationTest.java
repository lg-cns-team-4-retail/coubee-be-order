package com.coubee.service.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
public abstract class IntegrationTest {

    @Autowired private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer.stop();
        wireMockServer.start();

        // gRPC server removed
    }

    @AfterEach
    void afterEach() {
        wireMockServer.resetAll();
    }
}
