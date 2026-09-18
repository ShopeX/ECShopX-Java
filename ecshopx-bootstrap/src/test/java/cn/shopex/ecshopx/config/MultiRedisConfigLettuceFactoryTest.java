package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.lettuce.core.ClientOptions;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

class MultiRedisConfigLettuceFactoryTest {

	@Test
	void standaloneFactoryEnablesValidateConnectionAndTcpKeepAlive() {
		MultiRedisConfig config = new MultiRedisConfig();
		ReflectionTestUtils.setField(config, "commandTimeout", Duration.ofSeconds(5));
		ReflectionTestUtils.setField(config, "connectTimeout", Duration.ofSeconds(10));
		ReflectionTestUtils.setField(config, "validateConnection", true);

		LettuceConnectionFactory factory = config.standaloneFactory("127.0.0.1", 6379, "pwd", 1);

		assertTrue(factory.getValidateConnection());
		ClientOptions options = factory.getClientConfiguration().getClientOptions()
				.orElseThrow(() -> new AssertionError("client options expected"));
		assertTrue(options.isAutoReconnect());
		assertTrue(options.getSocketOptions().isKeepAlive());
	}
}
