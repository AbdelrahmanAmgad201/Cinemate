package org.example.watchparty;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// "test" profile overlays a throwaway jwt.public-key onto the base config so the context
// can build the JwtDecoder without the JWT_PUBLIC_KEY env var (see application-test.properties).
@SpringBootTest
@ActiveProfiles("test")
class WatchPartyApplicationTests {

    @Test
    void contextLoads() {
    }

}
