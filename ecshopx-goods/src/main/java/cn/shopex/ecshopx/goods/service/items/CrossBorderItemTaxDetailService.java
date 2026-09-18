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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.crossborder.domain.Taxstrategy;
import cn.shopex.ecshopx.crossborder.mapper.TaxstrategyMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CrossBorderItemTaxDetailService {

	private final ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;
	private final TaxstrategyMapper taxstrategyMapper;
	private final ObjectMapper objectMapper;

	public CrossBorderItemTaxDetailService(ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository, TaxstrategyMapper taxstrategyMapper,
			ObjectMapper objectMapper) {
		this.itemCrossBorderTaxQueryRepository = itemCrossBorderTaxQueryRepository;
		this.taxstrategyMapper = taxstrategyMapper;
		this.objectMapper = objectMapper;
	}

	public void applyCrossBorderTaxFields(Map<String, Object> detail, Items item) {
		if (detail == null || item == null) {
			return;
		}
		Object typeObj = detail.get("type");
		int type = typeObj instanceof Number ? ((Number) typeObj).intValue() : Integer.parseInt(String.valueOf(typeObj != null ? typeObj : 0));
		if (type != 1) {
			detail.put("tax_rate", 0);
			detail.put("cross_border_tax", 0);
			return;
		}
		int priceFen = item.getPrice() != null ? item.getPrice() : 0;
		BigDecimal ratePercent = itemCrossBorderTaxQueryRepository.resolveEffectiveTaxRatePercent(item, priceFen);
		detail.put("cross_border_tax_rate", ratePercent);
		BigDecimal price = BigDecimal.valueOf(priceFen);
		BigDecimal tax = price.multiply(ratePercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
		detail.put("cross_border_tax", tax.intValue());

		Long tsId = item.getTaxstrategyId();
		long companyId = item.getCompanyId() != null ? item.getCompanyId() : 0L;
		if (tsId != null && tsId > 0 && companyId > 0) {
			LambdaQueryWrapper<Taxstrategy> w = new LambdaQueryWrapper<>();
			w.eq(Taxstrategy::getId, tsId).eq(Taxstrategy::getCompanyId, companyId).last("LIMIT 1");
			Taxstrategy strat = taxstrategyMapper.selectOne(w);
			if (strat != null && StringUtils.hasText(strat.getTaxstrategyContent())) {
				try {
					JsonNode node = objectMapper.readTree(strat.getTaxstrategyContent());
					detail.put("tax_strategy", objectMapper.convertValue(node, Object.class));
				} catch (Exception e) {
					detail.put("tax_strategy", strat.getTaxstrategyContent());
				}
			}
		}
	}
}
