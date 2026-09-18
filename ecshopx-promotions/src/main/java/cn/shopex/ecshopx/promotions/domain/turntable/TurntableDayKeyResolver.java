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

package cn.shopex.ecshopx.promotions.domain.turntable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 业务日 day_key 解析（PRD §6.3.1）：可配置 Zone，禁止依赖 systemDefault。
 */
@Component
public class TurntableDayKeyResolver {

	private final ZoneId defaultZone;
	private final Clock clock;

	@Autowired
	public TurntableDayKeyResolver(
			@Value("${ecshopx.promotions.turntable.zone-id:Asia/Shanghai}") String zoneId) {
		this(zoneId, Clock.systemUTC());
	}

	TurntableDayKeyResolver(String zoneId, Clock clock) {
		ZoneId z;
		try {
			z = ZoneId.of(StringUtils.hasText(zoneId) ? zoneId.trim() : "Asia/Shanghai");
		} catch (Exception e) {
			z = ZoneId.of("Asia/Shanghai");
		}
		this.defaultZone = z;
		this.clock = clock == null ? Clock.systemUTC() : clock;
	}

	public ZoneId resolveZone(long companyId) {
		// 公司级时区后续可接入；一期用部署配置
		return defaultZone;
	}

	public String dayKey(long companyId) {
		return LocalDate.now(clock.withZone(resolveZone(companyId))).toString();
	}

	public String dayKey(long companyId, long epochSeconds) {
		return java.time.Instant.ofEpochSecond(epochSeconds)
				.atZone(resolveZone(companyId))
				.toLocalDate()
				.toString();
	}
}
