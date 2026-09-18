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

import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitDistributionPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍；alias 与 plan §4 {@code wxapp-datacube-visit-distribution} 一致。
 */
@Slf4j
public class NoopWxappDataCubeVisitDistributionPort implements WxappDataCubeVisitDistributionPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public JsonNode postVisitDistribution(String wxaAppId, String beginYmd, String endYmd) {
		int n = callCount.incrementAndGet();
		boolean appIdPresent = wxaAppId != null && !wxaAppId.isEmpty();
		log.info(
				"[cron-mock][wxapp-datacube-visit-distribution] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {appIdPresent, beginYmd, endYmd}));
		return JsonNodeFactory.instance.objectNode().set("list", JsonNodeFactory.instance.arrayNode());
	}
}
