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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallNormalItemPreRelService {

	private final PointsmallItemsMapper pointsmallItemsMapper;

	public PointsmallNormalItemPreRelService(PointsmallItemsMapper pointsmallItemsMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	public void apply(Map<String, Object> data, Map<String, Object> skuParams, long companyId, boolean forceCreate) {
		Object storeObj = skuParams.get("store");
		if (storeObj == null || !StringUtils.hasText(storeObj.toString())) {
			throw new ResourceException("库存为0-999999999的整数");
		}
		final int store;
		try {
			store = (int) toLong(storeObj);
		} catch (NumberFormatException e) {
			throw new ResourceException("库存为0-999999999的整数");
		}
		if (store < 0 || store > 999999999L) {
			throw new ResourceException("库存为0-999999999的整数");
		}
		if (skuParams.containsKey("cost_price")) {
			data.put("cost_price", moneyToFen(skuParams.get("cost_price")));
		}
		data.put("store", store);

		Long rowItemId = null;
		if (!forceCreate && skuParams.get("item_id") != null) {
			long rid = toLong(skuParams.get("item_id"));
			if (rid > 0) {
				rowItemId = rid;
			}
		}
		ensureUniqueItemBn(data, companyId, rowItemId);
	}

	private void ensureUniqueItemBn(Map<String, Object> data, long companyId, Long rowItemId) {
		String bn = str(data.get("item_bn"));
		if (!StringUtils.hasText(bn)) {
			bn = ("S" + UUID.randomUUID().toString().replace("-", "")).toUpperCase(Locale.ROOT).substring(0, 14);
			data.put("item_bn", bn);
		}
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getItemBn, bn).last("LIMIT 1");
		PointsmallItems existing = pointsmallItemsMapper.selectOne(w);
		if (existing == null || existing.getItemId() == null) {
			return;
		}
		if (rowItemId == null) {
			throw new ResourceException(bn + "商品编码重复，请添加正确的商品编码");
		}
		if (!rowItemId.equals(existing.getItemId())) {
			throw new ResourceException("商品编码重复，请添加正确的商品编码");
		}
	}

	private static int moneyToFen(Object v) {
		if (v == null) {
			return 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		return new BigDecimal(s).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
