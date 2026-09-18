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

package cn.shopex.ecshopx.promotions.integration.orders;

import cn.shopex.ecshopx.common.cron.port.TurntablePayGetTimesOnOrderPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 PHP {@code TurntableService::payGetTurntableTimes} 一致：读 {@code turntableConfigCompany_{cid}} 的
 * {@code shopping_full}，满额则对 {@code turntableUserSurplusTimes:CompanyId:{cid}} 做 hincrby。
 */
@Service
public class TurntablePayGetTimesOnOrderPortImpl implements TurntablePayGetTimesOnOrderPort {

	private final StringRedisTemplate stringRedisTemplate;

	public TurntablePayGetTimesOnOrderPortImpl(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public void payGetTimes(long userId, long companyId, int totalFeeFen) {
		if (userId <= 0L || companyId <= 0L) {
			return;
		}
		String configKey = "turntableConfigCompany_" + companyId;
		Object raw = stringRedisTemplate.opsForHash().get(configKey, "shopping_full");
		String shoppingFullYuan = raw == null ? null : String.valueOf(raw).trim();
		if (!StringUtils.hasText(shoppingFullYuan) || "-1".equals(shoppingFullYuan)) {
			return;
		}
		BigDecimal fullFen;
		try {
			fullFen = new BigDecimal(shoppingFullYuan).multiply(new BigDecimal(100));
		} catch (NumberFormatException e) {
			return;
		}
		if (fullFen.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		BigDecimal div =
				new BigDecimal(totalFeeFen).divide(fullFen, 0, RoundingMode.DOWN);
		if (div.compareTo(BigDecimal.ONE) < 0) {
			return;
		}
		long add = div.longValue();
		if (add <= 0L) {
			return;
		}
		String surplusKey = "turntableUserSurplusTimes:CompanyId:" + companyId;
		String field = "UserId:" + userId;
		stringRedisTemplate.opsForHash().increment(surplusKey, field, add);
	}
}
