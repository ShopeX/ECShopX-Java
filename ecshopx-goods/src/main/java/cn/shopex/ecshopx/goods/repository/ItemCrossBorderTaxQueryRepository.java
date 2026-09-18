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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.crossborder.domain.CrossBorderSet;
import cn.shopex.ecshopx.crossborder.domain.Taxstrategy;
import cn.shopex.ecshopx.crossborder.mapper.CrossBorderSetMapper;
import cn.shopex.ecshopx.crossborder.mapper.TaxstrategyMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 只读跨境税率解析（商品 / 类目 / 全局设置 / 税费策略分段），与 {@code ItemTaxRateService} 判定顺序一致。
 */
@Repository
public class ItemCrossBorderTaxQueryRepository {

	private final TaxstrategyMapper taxstrategyMapper;
	private final CrossBorderSetMapper crossBorderSetMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;

	public ItemCrossBorderTaxQueryRepository(TaxstrategyMapper taxstrategyMapper, CrossBorderSetMapper crossBorderSetMapper,
			ItemsCategoryRepository itemsCategoryRepository) {
		this.taxstrategyMapper = taxstrategyMapper;
		this.crossBorderSetMapper = crossBorderSetMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	public BigDecimal resolveEffectiveTaxRatePercent(Items item, int priceFen) {
		if (item == null || item.getType() == null || item.getType() != 1) {
			return BigDecimal.ZERO;
		}
		long companyId = item.getCompanyId() != null ? item.getCompanyId() : 0L;
		if (companyId <= 0) {
			return BigDecimal.ZERO;
		}

		if (item.getTaxstrategyId() != null && item.getTaxstrategyId() > 0 && item.getTaxationNum() != null && item.getTaxationNum() > 0) {
			BigDecimal fromStrategy = resolveTaxRateFromStrategy(item.getTaxstrategyId(), companyId, item.getTaxationNum(), priceFen);
			if (fromStrategy.compareTo(BigDecimal.ZERO) > 0) {
				return fromStrategy;
			}
		}

		if (StringUtils.hasText(item.getCrossborderTaxRate())) {
			BigDecimal p = parsePercent(item.getCrossborderTaxRate());
			if (p.compareTo(BigDecimal.ZERO) > 0) {
				return p;
			}
		}

		BigDecimal catRate = categoryCrossBorderRate(item, companyId);
		if (catRate.compareTo(BigDecimal.ZERO) > 0) {
			return catRate;
		}

		return globalCrossBorderRate(companyId);
	}

	private BigDecimal resolveTaxRateFromStrategy(long taxstrategyId, long companyId, int taxationNum, int taxableFeeFen) {
		LambdaQueryWrapper<Taxstrategy> w = new LambdaQueryWrapper<>();
		w.eq(Taxstrategy::getId, taxstrategyId).eq(Taxstrategy::getCompanyId, companyId).last("LIMIT 1");
		Taxstrategy row = taxstrategyMapper.selectOne(w);
		if (row == null || !StringUtils.hasText(row.getTaxstrategyContent())) {
			return BigDecimal.ZERO;
		}
		BigDecimal price = BigDecimal.valueOf(taxableFeeFen);
		BigDecimal onePrice = price.divide(BigDecimal.valueOf(taxationNum), 2, RoundingMode.DOWN).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
		// JSON 数组元素：start, end, tax_rate
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			com.fasterxml.jackson.databind.JsonNode arr = om.readTree(row.getTaxstrategyContent());
			if (!arr.isArray()) {
				return BigDecimal.ZERO;
			}
			for (com.fasterxml.jackson.databind.JsonNode seg : arr) {
				BigDecimal start = seg.has("start") ? new BigDecimal(seg.get("start").asText()) : BigDecimal.ZERO;
				BigDecimal end = seg.has("end") ? new BigDecimal(seg.get("end").asText()) : BigDecimal.ZERO;
				BigDecimal tr = seg.has("tax_rate") ? new BigDecimal(seg.get("tax_rate").asText()) : BigDecimal.ZERO;
				if (onePrice.compareTo(start) > 0 && onePrice.compareTo(end) <= 0) {
					return tr;
				}
			}
		} catch (Exception ignored) {
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal categoryCrossBorderRate(Items item, long companyId) {
		String ic = item.getItemCategory();
		if (!StringUtils.hasText(ic)) {
			return BigDecimal.ZERO;
		}
		try {
			long cid = Long.parseLong(ic.trim());
			Optional<ItemsCategory> c = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, cid);
			if (c.isEmpty() || !StringUtils.hasText(c.get().getCrossborderTaxRate())) {
				return BigDecimal.ZERO;
			}
			return parsePercent(c.get().getCrossborderTaxRate());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal globalCrossBorderRate(long companyId) {
		LambdaQueryWrapper<CrossBorderSet> w = new LambdaQueryWrapper<>();
		w.eq(CrossBorderSet::getCompanyId, companyId).last("LIMIT 1");
		CrossBorderSet row = crossBorderSetMapper.selectOne(w);
		if (row == null || !StringUtils.hasText(row.getTaxRate())) {
			return BigDecimal.ZERO;
		}
		return parsePercent(row.getTaxRate());
	}

	private static BigDecimal parsePercent(String raw) {
		if (!StringUtils.hasText(raw)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(raw.trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}
}
