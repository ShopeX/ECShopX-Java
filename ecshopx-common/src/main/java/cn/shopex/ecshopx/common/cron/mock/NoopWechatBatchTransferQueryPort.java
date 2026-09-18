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

import cn.shopex.ecshopx.common.port.weixin.WechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下不发起微信 TLS；与 {@code wechat-http} 别名一致供阶段 4 grep 断言。
 */
@Slf4j
public class NoopWechatBatchTransferQueryPort implements WechatBatchTransferQueryPort {

	private final AtomicInteger callCount = new AtomicInteger();
	private final ObjectMapper objectMapper;

	public NoopWechatBatchTransferQueryPort(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public JsonNode queryBalanceOrder(
			long companyId, WechatMerchantV3ApiMaterial material, String outBatchId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][wechat-http] called#{}, args=companyId={}, paymentNo={}",
				n, companyId, outBatchId);
		return objectMapper.createObjectNode();
	}
}
