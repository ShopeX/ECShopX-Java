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

package cn.shopex.ecshopx.shuyun.config;

import cn.shopex.ecshopx.shuyun.service.openplatform.TokenRefreshService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** O1 schedule：每天 04:15，对齐 PHP {@code dailyAt('4:15')}。 */
@Component
public class TokenRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(TokenRefreshScheduler.class);

	private final TokenRefreshService tokenRefreshService;

	public TokenRefreshScheduler(TokenRefreshService tokenRefreshService) {
		this.tokenRefreshService = tokenRefreshService;
	}

	@Scheduled(cron = "0 15 4 * * ?")
	public void refreshTokensDaily() {
		Map<String, Integer> stats = tokenRefreshService.run(null);
		log.info(
				"shuyun_open_platform_refresh_tokens attempted={} ok={} failed={}",
				stats.get("attempted"),
				stats.get("ok"),
				stats.get("failed"));
	}
}
