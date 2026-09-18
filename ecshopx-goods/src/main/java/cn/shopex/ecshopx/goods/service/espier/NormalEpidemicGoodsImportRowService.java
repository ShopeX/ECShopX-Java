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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NormalEpidemicGoodsImportRowService {

	private final ItemsRepository itemsRepository;
	private final ItemsMapper itemsMapper;

	public NormalEpidemicGoodsImportRowService(ItemsRepository itemsRepository, ItemsMapper itemsMapper) {
		this.itemsRepository = itemsRepository;
		this.itemsMapper = itemsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyRow(long companyId, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		String barcode = r.get("barcode");
		String itemBn = r.get("item_bn");
		String epidemicRaw = r.get("is_epidemic");
		if (!StringUtils.hasText(barcode)) {
			throw new BadRequestException("缺少必填字段: barcode");
		}
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("缺少必填字段: item_bn");
		}
		if (!StringUtils.hasText(epidemicRaw)) {
			throw new BadRequestException("缺少必填字段: is_epidemic");
		}
		int epidemicFlag = parseEpidemicFlag(epidemicRaw);
		List<Items> candidates = itemsRepository.listByCompanyIdAndBarcodeExact(companyId, barcode.trim());
		List<Items> matched = candidates.stream()
				.filter(it -> it.getItemBn() != null && itemBn.trim().equals(it.getItemBn().trim()))
				.toList();
		if (matched.isEmpty()) {
			throw new ResourceException("未找到与条形码及货号匹配的商品");
		}
		if (matched.size() > 1) {
			throw new ResourceException("条形码匹配到多条商品记录，请核对货号");
		}
		Items target = matched.get(0);
		Long itemId = target.getItemId();
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("未找到与条形码及货号匹配的商品");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Items> u = new LambdaUpdateWrapper<>();
		u.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId).set(Items::getIsEpidemic, epidemicFlag).set(Items::getUpdated, now);
		itemsMapper.update(null, u);
	}

	private static int parseEpidemicFlag(String raw) {
		String t = raw.trim();
		if ("1".equals(t) || "是".equals(t) || "Y".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
			return 1;
		}
		if ("0".equals(t) || "否".equals(t) || "N".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t)) {
			return 0;
		}
		try {
			int n = new BigDecimal(t).intValue();
			if (n == 1) {
				return 1;
			}
			if (n == 0) {
				return 0;
			}
		} catch (NumberFormatException ignored) {
		}
		throw new BadRequestException("是否设为疫情商品格式错误");
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}
}
