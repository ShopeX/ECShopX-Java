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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.openapi.OpenapiDigiOsProductStockPushPort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.espier.NormalGoodsStoreImportRowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 对齐 PHP {@code DigiOsStockPushService}。 */
@Service
public class DigiOsStockPushService {

	private final DigiOsProperties properties;
	private final ObjectMapper objectMapper;
	private final ItemsRepository itemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final NormalGoodsStoreImportRowService normalGoodsStoreImportRowService;
	private final DigiOsApiLogService apiLogService;

	public DigiOsStockPushService(
			DigiOsProperties properties,
			ObjectMapper objectMapper,
			ItemsRepository itemsRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			NormalGoodsStoreImportRowService normalGoodsStoreImportRowService,
			DigiOsApiLogService apiLogService) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.itemsRepository = itemsRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.normalGoodsStoreImportRowService = normalGoodsStoreImportRowService;
		this.apiLogService = apiLogService;
	}

	public Map<String, Object> pushStoreStock(long companyId, Object skuListRaw) {
		long started = System.currentTimeMillis();
		List<Map<String, Object>> rows =
				DigiOsStockPushMapper.parseAndValidate(skuListRaw, properties.getStockPushMaxRows(), objectMapper);

		List<String> itemBns = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			itemBns.add(String.valueOf(row.get("sku_id")));
		}
		Map<String, Items> itemBnMap = itemsRepository.mapItemsByItemBnIn(itemBns);

		List<String> notFindSku = new ArrayList<>();
		List<Map<String, Object>> updateError = new ArrayList<>();

		for (Map<String, Object> row : rows) {
			String skuId = String.valueOf(row.get("sku_id"));
			String shopCode = String.valueOf(row.get("shop_code"));
			Items item = itemBnMap.get(skuId);
			if (item == null) {
				notFindSku.add(skuId);
				continue;
			}
			Map<String, Object> distInfo =
					distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, shopCode);
			long distributorId = extractLong(distInfo, "distributor_id");
			if (distributorId <= 0L) {
				Map<String, Object> err = new LinkedHashMap<>();
				err.put("sku_id", skuId);
				err.put("shop_code", shopCode);
				err.put("error", "门店不存在");
				updateError.add(err);
				continue;
			}
			Map<String, Object> importRow = new LinkedHashMap<>();
			importRow.put("did", String.valueOf(distributorId));
			importRow.put("distributor_id", String.valueOf(distributorId));
			importRow.put("item_bn", skuId);
			importRow.put("store", String.valueOf(row.get("stock")));
			try {
				normalGoodsStoreImportRowService.applyRow(companyId, importRow);
			} catch (BadRequestException e) {
				Map<String, Object> err = new LinkedHashMap<>();
				err.put("sku_id", skuId);
				err.put("shop_code", shopCode);
				err.put("error", e.getMessage());
				updateError.add(err);
			} catch (Exception e) {
				Map<String, Object> err = new LinkedHashMap<>();
				err.put("sku_id", skuId);
				err.put("shop_code", shopCode);
				err.put("error", "更新失败");
				updateError.add(err);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("not_find_sku", notFindSku);
		data.put("not_update_sku", List.of());
		data.put("update_error", updateError);

		int runtimeMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started);
		apiLogService.writeInbound(
				companyId,
				OpenapiDigiOsProductStockPushPort.OPENAPI_METHOD,
				200,
				"success",
				runtimeMs,
				Map.of("sku_list", rows),
				data,
				null);
		return data;
	}

	private static long extractLong(Map<String, Object> map, String key) {
		if (map == null || map.isEmpty()) {
			return 0L;
		}
		Object raw = map.get(key);
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
