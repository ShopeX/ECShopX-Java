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

package cn.shopex.ecshopx.payment.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每分钟检查水位；达到间隔则全量刷新 token。 */
@Component
public class DoumenIntlTokenRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(DoumenIntlTokenRefreshScheduler.class);

	private final DoumenIntlScheduledTokenRefreshRunner runner;

	public DoumenIntlTokenRefreshScheduler(DoumenIntlScheduledTokenRefreshRunner runner) {
		this.runner = runner;
	}

	@Scheduled(cron = "0 * * * * ?")
	public void refreshTokensWhenDue() {
		if (!runner.shouldRunScheduledRefresh()) {
			return;
		}
		Map<String, Integer> stats = runner.run(null);
		log.info(
				"doumen_intl_refresh_tokens attempted={} ok={} failed={}",
				stats.get("attempted"),
				stats.get("ok"),
				stats.get("failed"));
	}
}
