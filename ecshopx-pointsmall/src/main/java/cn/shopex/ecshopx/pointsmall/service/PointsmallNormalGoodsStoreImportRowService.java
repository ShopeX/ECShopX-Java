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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallNormalGoodsStoreImportRowService {

	private static final String STORE_VALIDATION_MSG = "库存为0-999999999的整数";

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService;

	public PointsmallNormalGoodsStoreImportRowService(
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemStoreRedisWriteService = pointsmallItemStoreRedisWriteService;
	}

	public void acceptRow(long companyId, Map<String, Object> row) {
		String itemBn = trim(row.get("item_bn"));
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("请填写商品编码");
		}
		int store = parseStore(row.get("store"));

		PointsmallItems item =
				pointsmallItemsMapper.selectOne(
						new LambdaQueryWrapper<PointsmallItems>()
								.eq(PointsmallItems::getCompanyId, companyId)
								.eq(PointsmallItems::getItemBn, itemBn)
								.last("LIMIT 1"));
		if (item == null || item.getItemId() == null) {
			throw new ResourceException("商品不存在");
		}
		long itemId = item.getItemId();
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<PointsmallItems> u = new LambdaUpdateWrapper<PointsmallItems>()
				.eq(PointsmallItems::getItemId, itemId)
				.set(PointsmallItems::getStore, store)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, u);
		pointsmallItemStoreRedisWriteService.save(itemId, store);
	}

	private static int parseStore(Object v) {
		if (v == null) {
			throw new BadRequestException(STORE_VALIDATION_MSG);
		}
		if (v instanceof BigDecimal bd) {
			try {
				long lv = bd.longValueExact();
				if (lv < 0L || lv > 999_999_999L) {
					throw new BadRequestException(STORE_VALIDATION_MSG);
				}
				return (int) lv;
			} catch (ArithmeticException e) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
		}
		if (v instanceof Number n) {
			long lv = n.longValue();
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
			return (int) lv;
		}
		String s = String.valueOf(v).trim();
		try {
			long lv = Long.parseLong(s);
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
			return (int) lv;
		} catch (NumberFormatException e) {
			throw new BadRequestException(STORE_VALIDATION_MSG);
		}
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
