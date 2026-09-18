/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * Multi-database Redis wiring. Custom {@link LettuceConnectionFactory} beans intentionally bypass
 * Spring Boot auto-config, so Docker/idle {@code Connection reset} must be handled here:
 * TCP keepalive + {@code validateConnection} before use.
 */
@Configuration
public class MultiRedisConfig {

	@Value("${ecshopx.redis.lettuce.command-timeout:5s}")
	private Duration commandTimeout;

	@Value("${ecshopx.redis.lettuce.connect-timeout:10s}")
	private Duration connectTimeout;

	@Value("${ecshopx.redis.lettuce.validate-connection:true}")
	private boolean validateConnection;

	@Bean(name = "companysRedisConnectionFactory")
	public RedisConnectionFactory companysRedisConnectionFactory(
			@Value("${spring.data.redis.host:127.0.0.1}") String host,
			@Value("${spring.data.redis.port:6379}") int port,
			@Value("${spring.data.redis.password:}") String password,
			@Value("${ecshopx.redis.companys.database:0}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	/**
	 * Same host/port/password as {@link #companysRedisConnectionFactory}, but database from
	 * {@code spring.data.redis.database} (default 0) for shared default Redis DB parity.
	 */
	@Bean(name = "springDataRedisConnectionFactory")
	public RedisConnectionFactory springDataRedisConnectionFactory(
			@Value("${spring.data.redis.host:127.0.0.1}") String host,
			@Value("${spring.data.redis.port:6379}") int port,
			@Value("${spring.data.redis.password:}") String password,
			@Value("${spring.data.redis.database:0}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "prismRedisConnectionFactory")
	public RedisConnectionFactory prismRedisConnectionFactory(
			@Value("${spring.data.redis.host:127.0.0.1}") String host,
			@Value("${spring.data.redis.port:6379}") int port,
			@Value("${spring.data.redis.password:}") String password,
			@Value("${ecshopx.redis.prism.database:2}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "datacubeRedisConnectionFactory")
	public RedisConnectionFactory datacubeRedisConnectionFactory(
			@Value("${spring.data.redis.host:127.0.0.1}") String host,
			@Value("${spring.data.redis.port:6379}") int port,
			@Value("${spring.data.redis.password:}") String password,
			@Value("${ecshopx.redis.datacube.database:0}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "depositRedisConnectionFactory")
	public RedisConnectionFactory depositRedisConnectionFactory(
			@Value("${ecshopx.redis.deposit.host:${spring.data.redis.host:127.0.0.1}}") String host,
			@Value("${ecshopx.redis.deposit.port:${spring.data.redis.port:6379}}") int port,
			@Value("${ecshopx.redis.deposit.password:${spring.data.redis.password:}}") String password,
			@Value("${ecshopx.redis.deposit.database:1}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "redisTemplate")
	public RedisTemplate<Object, Object> redisTemplate(
			@Qualifier("companysRedisConnectionFactory") RedisConnectionFactory factory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		return template;
	}

	@Bean(name = "companysRedisTemplate")
	@Primary
	public StringRedisTemplate companysRedisTemplate(
			@Qualifier("companysRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "sharedStringRedisTemplate")
	public StringRedisTemplate sharedStringRedisTemplate(
			@Qualifier("springDataRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "dispatchBusStringRedisTemplate")
	public StringRedisTemplate dispatchBusStringRedisTemplate(
			@Qualifier("springDataRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "prismRedisTemplate")
	public StringRedisTemplate prismRedisTemplate(
			@Qualifier("prismRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "datacubeStringRedisTemplate")
	public StringRedisTemplate datacubeStringRedisTemplate(
			@Qualifier("datacubeRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "depositStringRedisTemplate")
	public StringRedisTemplate depositStringRedisTemplate(
			@Qualifier("depositRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	/**
	 * Members Redis: optional dedicated host/port; database defaults to {@code spring.data.redis.database}
	 * so member-scoped keys (e.g. sync throttling) land in the same logical DB as the platform default Redis.
	 */
	@Bean(name = "membersRedisConnectionFactory")
	public RedisConnectionFactory membersRedisConnectionFactory(
			@Value("${ecshopx.redis.members.host:${spring.data.redis.host:127.0.0.1}}") String host,
			@Value("${ecshopx.redis.members.port:${spring.data.redis.port:6379}}") int port,
			@Value("${ecshopx.redis.members.password:${spring.data.redis.password:}}") String password,
			@Value("${ecshopx.redis.members.database:${spring.data.redis.database:0}}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "membersStringRedisTemplate")
	public StringRedisTemplate membersStringRedisTemplate(
			@Qualifier("membersRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "espierRedisConnectionFactory")
	public RedisConnectionFactory espierRedisConnectionFactory(
			@Value("${ecshopx.redis.espier.host:${spring.data.redis.host:127.0.0.1}}") String host,
			@Value("${ecshopx.redis.espier.port:${spring.data.redis.port:6379}}") int port,
			@Value("${ecshopx.redis.espier.password:${spring.data.redis.password:}}") String password,
			@Value("${ecshopx.redis.espier.database:${spring.data.redis.database:0}}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "espierStringRedisTemplate")
	public StringRedisTemplate espierStringRedisTemplate(
			@Qualifier("espierRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	@Bean(name = "wechatRedisConnectionFactory")
	public RedisConnectionFactory wechatRedisConnectionFactory(
			@Value("${ecshopx.redis.wechat.host:${spring.data.redis.host:127.0.0.1}}") String host,
			@Value("${ecshopx.redis.wechat.port:${spring.data.redis.port:6379}}") int port,
			@Value("${ecshopx.redis.wechat.password:${spring.data.redis.password:}}") String password,
			@Value("${ecshopx.redis.wechat.database:${spring.data.redis.database:0}}") int database) {
		return standaloneFactory(host, port, password, database);
	}

	@Bean(name = "wechatStringRedisTemplate")
	public StringRedisTemplate wechatStringRedisTemplate(
			@Qualifier("wechatRedisConnectionFactory") RedisConnectionFactory factory) {
		StringRedisTemplate t = new StringRedisTemplate();
		t.setConnectionFactory(factory);
		return t;
	}

	LettuceConnectionFactory standaloneFactory(String host, int port, String password, int database) {
		RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
		standalone.setDatabase(database);
		if (StringUtils.hasText(password)) {
			standalone.setPassword(RedisPassword.of(password));
		}
		SocketOptions socketOptions = SocketOptions.builder()
				.keepAlive(true)
				.connectTimeout(connectTimeout)
				.build();
		ClientOptions clientOptions = ClientOptions.builder()
				.socketOptions(socketOptions)
				.autoReconnect(true)
				.build();
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.clientOptions(clientOptions)
				.commandTimeout(commandTimeout)
				.build();
		LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
		factory.setValidateConnection(validateConnection);
		return factory;
	}
}
