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

import cn.shopex.ecshopx.companys.service.setting.CompanysPharmaIndustrySettingReadService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.dispatch.ItemBatchEditStatusEventJobEnqueuePort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsBatchApproveStatusService {

	private final GoodsItemsBatchStatusQueryParamsService goodsItemsBatchStatusQueryParamsService;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsRepository itemsRepository;
	private final ItemsMedicineRepository itemsMedicineRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService;
	private final ItemBatchEditStatusEventJobEnqueuePort itemBatchEditStatusEventJobEnqueuePort;
	private final ObjectMapper objectMapper;

	public GoodsItemsBatchApproveStatusService(GoodsItemsBatchStatusQueryParamsService goodsItemsBatchStatusQueryParamsService,
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsRepository itemsRepository,
			ItemsMedicineRepository itemsMedicineRepository,
			SupplierItemsRepository supplierItemsRepository,
			CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService,
			ItemBatchEditStatusEventJobEnqueuePort itemBatchEditStatusEventJobEnqueuePort,
			ObjectMapper objectMapper) {
		this.goodsItemsBatchStatusQueryParamsService = goodsItemsBatchStatusQueryParamsService;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsRepository = itemsRepository;
		this.itemsMedicineRepository = itemsMedicineRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.companysPharmaIndustrySettingReadService = companysPharmaIndustrySettingReadService;
		this.itemBatchEditStatusEventJobEnqueuePort = itemBatchEditStatusEventJobEnqueuePort;
		this.objectMapper = objectMapper;
	}

	public void batchUpdate(HttpServletRequest request, long companyId, long operatorId, String operatorType, Long merchantId,
			Map<String, Object> merged) {
		Object q = merged.get("query");
		if (isQueryTruthy(q)) {
			Map<String, Object> queryMapOrNull;
			if (q instanceof Map<?, ?> rawMap) {
				queryMapOrNull = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : rawMap.entrySet()) {
					queryMapOrNull.put(String.valueOf(e.getKey()), e.getValue());
				}
			} else if (q instanceof String s) {
				queryMapOrNull = decodeQueryJsonStringToMapOrNull(s);
			} else {
				throw new BadRequestException("参数类型错误");
			}
			Optional<Map<String, Object>> paramsOpt = goodsItemsBatchStatusQueryParamsService.tryBuildParams(companyId, operatorId, operatorType,
					merchantId, queryMapOrNull);
			if (paramsOpt.isEmpty()) {
				return;
			}
			Map<String, Object> params = new LinkedHashMap<>(paramsOpt.get());
			params.remove("isGetSkuList");
			List<Long> goodsIds = itemsListQueryRepository.listDistinctGoodsIdsByFullParams(params);
			List<Map<String, Object>> builtItems = new ArrayList<>();
			for (Long gid : goodsIds) {
				if (gid != null && gid > 0) {
					builtItems.add(Map.of("goods_id", gid));
				}
			}
			merged.put("items", builtItems);
		} else if (!isQueryTruthy(merged.get("items"))) {
			throw new BadRequestException("未指定商品");
		}

		List<Map<String, Object>> itemsList = normalizeItemsList(merged.get("items"));
		validateItemsAndStatus(itemsList, merged.get("status"));

		String statusString = merged.get("status").toString().trim();
		updateItemsStatus(companyId, itemsList, statusString);
	}

	/** Invalid JSON or a non-object root yields {@code null} (same outcome as treating the filter input as absent). */
	private Map<String, Object> decodeQueryJsonStringToMapOrNull(String s) {
		try {
			JsonNode node = objectMapper.readTree(s);
			if (node == null || !node.isObject()) {
				return null;
			}
			return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isQueryTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b && !b) {
			return false;
		}
		if (v instanceof Number n && n.doubleValue() == 0.0) {
			return false;
		}
		if (v instanceof String s) {
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return true;
	}

	private static List<Map<String, Object>> normalizeItemsList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("商品id必填");
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				throw new BadRequestException("商品id必填");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
		}
		return out;
	}

	private static void validateItemsAndStatus(List<Map<String, Object>> items, Object statusRaw) {
		if (statusRaw == null || !StringUtils.hasText(statusRaw.toString().trim())) {
			throw new BadRequestException("状态必填");
		}
		for (Map<String, Object> row : items) {
			if (!row.containsKey("goods_id") || row.get("goods_id") == null) {
				throw new BadRequestException("商品id必填");
			}
			Object g = row.get("goods_id");
			if (g instanceof String gs && !StringUtils.hasText(gs.trim())) {
				throw new BadRequestException("商品id必填");
			}
		}
	}

	public void updateItemsStatusForOpenapi(long companyId, List<Map<String, Object>> items, String statusString) {
		updateItemsStatus(companyId, items, statusString);
	}

	private void updateItemsStatus(long companyId, List<Map<String, Object>> items, String statusString) {
		List<Long> goodsIdList = items.stream().map(r -> parseLongGoodsId(r.get("goods_id"))).distinct().toList();

		if ("onsale".equals(statusString)) {
			List<Long> supIds = itemsRepository.listSupplierItemIdsGe1ByCompanyAndGoodsIds(companyId, goodsIdList);
			if (!supIds.isEmpty()) {
				long c = supplierItemsRepository.countSupplierItemsForDistributorUpdate(companyId, true, supIds);
				if (c > 0) {
					throw new ResourceException("供应商商品不可售");
				}
			}
		}

		if (!"instock".equals(statusString)) {
			List<Items> skuRows = itemsRepository.listItemIdAndItemNameByCompanyAndGoodsIds(companyId, goodsIdList);
			List<Long> itemIds = skuRows.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
			if (itemsMedicineRepository.existsPrescriptionByCompanyAndItemIds(companyId, itemIds)) {
				Map<String, Object> med = companysPharmaIndustrySettingReadService.getMedicineSetting(companyId);
				if (!isPharmaIndustryEnabled(med)) {
					throw new ResourceException("处方药商品需要开启医药行业配置才能上架");
				}
			}
			Optional<ItemsMedicine> pending = itemsMedicineRepository.findFirstPrescriptionPendingAuditByCompanyAndItemIds(companyId, itemIds);
			if (pending.isPresent()) {
				Long hitItemId = pending.get().getItemId();
				String name = "";
				for (Items it : skuRows) {
					if (hitItemId != null && hitItemId.equals(it.getItemId())) {
						name = it.getItemName() != null ? it.getItemName() : "";
						break;
					}
				}
				throw new ResourceException("处方药商品【" + name + "】审核通过后才能上架");
			}
		}

		for (Map<String, Object> row : items) {
			long goodsId = parseLongGoodsId(row.get("goods_id"));
			itemsRepository.updateApproveStatusByCompanyAndGoodsId(companyId, goodsId, statusString);
			itemBatchEditStatusEventJobEnqueuePort.enqueueAfterApproveStatusUpdate(companyId, goodsId, statusString);
		}
	}

	private static boolean isPharmaIndustryEnabled(Map<String, Object> medicineSetting) {
		Object v = medicineSetting.get("is_pharma_industry");
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString();
		return !s.isEmpty() && !"0".equals(s);
	}

	private static long parseLongGoodsId(Object g) {
		if (g instanceof Number n) {
			return n.longValue();
		}
		String s = g.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("商品id必填");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品id必填");
		}
	}
}
