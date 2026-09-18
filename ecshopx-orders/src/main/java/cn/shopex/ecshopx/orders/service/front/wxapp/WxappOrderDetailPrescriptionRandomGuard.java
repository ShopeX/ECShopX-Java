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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailPrescriptionRandomGuard {

	private static final Logger log = LoggerFactory.getLogger(WxappOrderDetailPrescriptionRandomGuard.class);

	private final StringRedisTemplate companysRedisTemplate;

	public WxappOrderDetailPrescriptionRandomGuard(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void logIfMiss(String orderId, String random) {
		String key =
				"dianwu_prescription_order_random:"
						+ (orderId == null ? "" : orderId.trim())
						+ (random == null ? "" : random.trim());
		String v = companysRedisTemplate.opsForValue().get(key);
		if (v == null) {
			log.info("prescriptionOrderRandom miss key={}", key);
		}
	}
}
