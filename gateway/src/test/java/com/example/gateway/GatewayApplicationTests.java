package com.example.gateway;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class GatewayApplicationTests {

    @MockitoBean
    private ProxyManager<String> proxyManager;

    @Test
    void contextLoads() {
    }

}
