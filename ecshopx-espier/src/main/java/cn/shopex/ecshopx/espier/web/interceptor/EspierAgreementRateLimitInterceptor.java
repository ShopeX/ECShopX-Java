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

package cn.shopex.ecshopx.espier.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class EspierAgreementRateLimitInterceptor implements HandlerInterceptor {

	private static final String RATE_BODY =
			"{\"message\":\"Too Many Attempts.\",\"status_code\":429}";

	private final StringRedisTemplate redis;

	public EspierAgreementRateLimitInterceptor(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		String ip = request.getHeader("X-Forwarded-For");
		if (ip != null && !ip.isEmpty()) {
			ip = ip.split(",")[0].trim();
		} else {
			ip = request.getRemoteAddr();
		}
		String key = "espier:agreement:throttle:" + ip;
		Long c = redis.opsForValue().increment(key);
		if (c != null && c == 1L) {
			redis.expire(key, Duration.ofMinutes(1));
		}
		if (c != null && c > 30) {
			response.setStatus(429);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType("application/json;charset=UTF-8");
			response.getOutputStream().write(RATE_BODY.getBytes(StandardCharsets.UTF_8));
			return false;
		}
		return true;
	}
}
