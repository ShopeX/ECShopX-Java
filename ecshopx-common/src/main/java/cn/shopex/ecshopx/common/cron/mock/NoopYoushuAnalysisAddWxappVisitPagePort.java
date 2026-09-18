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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍；alias 与 plan §4 {@code youshu-analysis-wxapp-visit-page} 一致。
 */
@Slf4j
public class NoopYoushuAnalysisAddWxappVisitPagePort implements YoushuAnalysisAddWxappVisitPagePort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void addWxappVisitPage(
			String dataSourceId, JsonNode visitData, YoushuOpenApiCredentials credentials) {
		int n = callCount.incrementAndGet();
		int listSize = visitData != null && visitData.path("list").isArray() ? visitData.path("list").size() : -1;
		log.info(
				"[cron-mock][youshu-analysis-wxapp-visit-page] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {dataSourceId, listSize, credentials}));
	}
}
