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

package cn.shopex.ecshopx.crossborder.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.crossborder.domain.CrossBorderSet;
import cn.shopex.ecshopx.crossborder.mapper.CrossBorderSetMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CrossBorderSetSaveService {

	private final CrossBorderSetMapper crossBorderSetMapper;

	public CrossBorderSetSaveService(CrossBorderSetMapper crossBorderSetMapper) {
		this.crossBorderSetMapper = crossBorderSetMapper;
	}

	public void save(long companyId, Map<String, Object> merged) {
		String taxRate = parseRequiredTaxRate(merged.get("tax_rate"));
		String quotaTipParsed = parseOptionalStringField(merged.get("quota_tip"));
		Integer showParsed = parseCrossborderShow(merged.get("crossborder_show"));
		String logisticsParsed = parseOptionalStringField(merged.get("logistics"));

		CrossBorderSet existing = crossBorderSetMapper.selectOne(
				Wrappers.<CrossBorderSet>lambdaQuery()
						.eq(CrossBorderSet::getCompanyId, companyId)
						.last("LIMIT 1"));

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			CrossBorderSet entity = new CrossBorderSet();
			entity.setCompanyId(companyId);
			entity.setTaxRate(taxRate);
			entity.setQuotaTip(quotaTipParsed);
			if (showParsed != null) {
				entity.setCrossborderShow(showParsed);
			}
			if (logisticsParsed != null) {
				entity.setLogistics(logisticsParsed);
			}
			entity.setCreated(now);
			entity.setUpdated(now);
			crossBorderSetMapper.insert(entity);
			Long id = entity.getId();
			if (id == null || id <= 0) {
				throw new ResourceException("操作失败");
			}
		} else {
			LambdaUpdateWrapper<CrossBorderSet> uw = new LambdaUpdateWrapper<CrossBorderSet>()
					.eq(CrossBorderSet::getCompanyId, companyId)
					.set(CrossBorderSet::getTaxRate, taxRate)
					.set(CrossBorderSet::getQuotaTip, quotaTipParsed)
					.set(CrossBorderSet::getCrossborderShow, showParsed)
					.set(CrossBorderSet::getLogistics, logisticsParsed)
					.set(CrossBorderSet::getUpdated, now);
			int rows = crossBorderSetMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("操作失败");
			}
		}
	}

	private static String parseRequiredTaxRate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("税率不能为空");
		}
		String taxRate;
		if (raw instanceof String s) {
			taxRate = s.trim();
		} else {
			taxRate = String.valueOf(raw).trim();
		}
		if (taxRate.isEmpty()) {
			throw new BadRequestException("税率不能为空");
		}
		return taxRate;
	}

	private static String parseOptionalStringField(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static Integer parseCrossborderShow(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
