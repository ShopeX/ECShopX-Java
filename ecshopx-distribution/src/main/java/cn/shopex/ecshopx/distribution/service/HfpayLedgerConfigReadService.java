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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayLedgerConfig;
import cn.shopex.ecshopx.hfpay.mapper.HfpayLedgerConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HfpayLedgerConfigReadService {

	private final HfpayLedgerConfigMapper hfpayLedgerConfigMapper;

	public HfpayLedgerConfigReadService(HfpayLedgerConfigMapper hfpayLedgerConfigMapper) {
		this.hfpayLedgerConfigMapper = hfpayLedgerConfigMapper;
	}

	public void assertLedgerOpenForShopOpenSplit(long companyId) {
		LambdaQueryWrapper<HfpayLedgerConfig> w = new LambdaQueryWrapper<>();
		w.eq(HfpayLedgerConfig::getCompanyId, companyId).last("LIMIT 1");
		HfpayLedgerConfig row = hfpayLedgerConfigMapper.selectOne(w);
		if (row == null || !"true".equalsIgnoreCase(String.valueOf(row.getIsOpen()))) {
			throw new ResourceException("请先在分账及结算-基础配置中开启分账");
		}
	}

	public void assertOpenAndRateAllowedIfNeeded(long companyId, Map<String, Object> merged) {
		if (!truthyIsOpen(merged.get("is_open"))) {
			return;
		}
		LambdaQueryWrapper<HfpayLedgerConfig> w = new LambdaQueryWrapper<>();
		w.eq(HfpayLedgerConfig::getCompanyId, companyId).last("LIMIT 1");
		HfpayLedgerConfig row = hfpayLedgerConfigMapper.selectOne(w);
		if (row == null || !"true".equalsIgnoreCase(String.valueOf(row.getIsOpen()))) {
			throw new ResourceException("未开启分账配置");
		}
		Object rateObj = merged.get("rate");
		int rate = 0;
		if (rateObj instanceof Number n) {
			rate = n.intValue();
		} else if (rateObj != null && !rateObj.toString().isBlank()) {
			rate = Integer.parseInt(rateObj.toString().trim());
		}
		if (rate < 0 || rate > 3000) {
			throw new ResourceException("平台服务费率超出允许范围");
		}
	}

	private static boolean truthyIsOpen(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}
}
