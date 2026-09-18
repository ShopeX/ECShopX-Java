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

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DistributorItemsDetailMergeService {

	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;

	public DistributorItemsDetailMergeService(PlatformItemsDetailCoreService platformItemsDetailCoreService,
			DistributorItemsRepository distributorItemsRepository,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort) {
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.distributorItemsRepository = distributorItemsRepository;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
	}

	public Map<String, Object> merge(long companyId, long itemId, long distributorId, String authorizerAppId, String productModel) {
		return merge(companyId, itemId, distributorId, authorizerAppId, productModel, List.of());
	}

	public Map<String, Object> merge(long companyId, long itemId, long distributorId, String authorizerAppId, String productModel,
			List<Long> limitItemIds) {
		List<Long> lim = limitItemIds == null ? List.of() : limitItemIds;
		Map<String, Object> detail = platformItemsDetailCoreService.build(companyId, itemId, authorizerAppId, lim);
		if (detail == null || detail.isEmpty() || detail.get("item_id") == null) {
			return detail;
		}

		detail.put("distributor_id", distributorId);

		long defaultItemId = toLong(detail.get("default_item_id"));
		if (defaultItemId <= 0) {
			defaultItemId = itemId;
		}
		List<Long> skuIds = new ArrayList<>();
		skuIds.add(itemId);
		List<Map<String, Object>> specItems = extractSpecItemsList(detail.get("spec_items"));
		if (specItems != null) {
			for (Map<String, Object> row : specItems) {
				skuIds.add(toLong(row.get("item_id")));
			}
		}

		List<DistributorItems> distRows = distributorItemsRepository.listByCompanyDistributorAndSkuScope(companyId, distributorId, defaultItemId, skuIds);
		if (distRows.isEmpty()) {
			detail.put("distributor_sale_status", "platform".equals(productModel));
			fillDistributorInfo(companyId, distributorId, detail);
			return detail;
		}

		Map<Long, DistributorItems> bySku = new HashMap<>();
		for (DistributorItems di : distRows) {
			if (di.getItemId() != null) {
				bySku.put(di.getItemId(), di);
			}
		}

		DistributorItems mainDist = bySku.get(itemId);
		if (mainDist == null) {
			for (DistributorItems di : distRows) {
				if (Objects.equals(di.getDefaultItemId(), defaultItemId) && Objects.equals(di.getItemId(), itemId)) {
					mainDist = di;
					break;
				}
			}
		}

		if (mainDist != null) {
			applyShopOverlay(detail, mainDist, distributorId, true, true);
			detail.put("item_total_store", toInt(detail.get("store")));
		} else {
			detail.put("item_total_store", toInt(detail.get("store")));
		}

		if (specItems != null) {
			List<Map<String, Object>> itemDataSpecItems = new ArrayList<>();
			for (Map<String, Object> row : specItems) {
				row.put("distributor_id", distributorId);
				long sid = toLong(row.get("item_id"));
				DistributorItems dr = bySku.get(sid);
				if (dr != null) {
					row.put("item_name", detail.get("item_name"));
					applyShopOverlay(row, dr, distributorId, false, true);
				} else {
					row.put("store", 0);
					row.put("approve_status", "instock");
				}
				itemDataSpecItems.add(row);
			}
			detail.put("spec_items", itemDataSpecItems);
			specItems = itemDataSpecItems;
		}

		DistributorItems saleStatusRef = mainDist != null ? mainDist : distRows.get(0);
		recalcTotalsAndApprove(detail, specItems, productModel, saleStatusRef);
		fillDistributorInfo(companyId, distributorId, detail);
		return detail;
	}

	private void applyShopOverlay(Map<String, Object> target, DistributorItems di, long distributorId, boolean mainRow, boolean replaceApprove) {
		boolean totalStore = truthy(di.getIsTotalStore());
		if (!totalStore) {
			if (di.getStore() != null) {
				target.put("store", di.getStore().intValue());
			}
			if (di.getPrice() != null) {
				target.put("price", di.getPrice().intValue());
			}
		}

		if (replaceApprove) {
			if (!truthy(di.getIsCanSale())) {
				target.put("approve_status", "instock");
			}
			if (truthy(di.getIsCanSale()) && !totalStore) {
				target.put("approve_status", "onsale");
			}
		}

		target.put("goods_can_sale", di.getGoodsCanSale());
		target.put("is_can_sale", di.getIsCanSale());
		long distId = di.getDistributorId() != null ? di.getDistributorId() : distributorId;
		target.put("distributor_id", distId);
		target.put("is_total_store", totalStore);

		if (di.getIsSelfDelivery() != null) {
			target.put("is_self_delivery", di.getIsSelfDelivery());
		}
		if (di.getIsExpressDelivery() != null) {
			target.put("is_express_delivery", di.getIsExpressDelivery());
		}
		if (di.getIsShow() != null && mainRow) {
			target.put("is_show", di.getIsShow());
		}
	}

	private void recalcTotalsAndApprove(Map<String, Object> detail, List<Map<String, Object>> specItems, String productModel, DistributorItems mainDist) {
		boolean saleStatus = computeDistributorSaleStatus(productModel, mainDist, detail);
		detail.put("distributor_sale_status", saleStatus);

		if (specItems == null || specItems.isEmpty()) {
			return;
		}
		int totalStore = 0;
		int totalSales = 0;
		List<String> statuses = new ArrayList<>();
		for (Map<String, Object> row : specItems) {
			totalStore += toInt(row.get("store"));
			totalSales += toInt(row.get("sales"));
			statuses.add(str(row.get("approve_status")));
		}
		detail.put("item_total_store", totalStore);
		detail.put("item_total_sales", totalSales);
		detail.put("approve_status", aggregateApproveStatus(statuses));
	}

	private static boolean computeDistributorSaleStatus(String productModel, DistributorItems mainDist, Map<String, Object> detail) {
		if (mainDist == null) {
			return "platform".equals(productModel);
		}
		boolean canSale = truthy(mainDist.getIsCanSale());
		boolean goodsCanSale = truthy(mainDist.getGoodsCanSale());
		boolean totalStore = truthy(mainDist.getIsTotalStore());
		String ap = str(detail.get("approve_status"));
		boolean platformOnSale = "onsale".equals(ap) || "offline_sale".equals(ap);
		return canSale && goodsCanSale && (totalStore || platformOnSale);
	}

	private static String aggregateApproveStatus(List<String> statuses) {
		if (statuses.contains("onsale")) {
			return "onsale";
		}
		if (statuses.contains("only_show")) {
			return "only_show";
		}
		if (statuses.contains("offline_sale")) {
			return "offline_sale";
		}
		return "instock";
	}

	private void fillDistributorInfo(long companyId, long distributorId, Map<String, Object> detail) {
		detail.put(
				"distributor_info",
				GoodsItemsDetailDistributorInfoLoader.load(adminOrderDetailDistributionSupportPort, companyId, distributorId));
	}

	private static boolean truthy(Boolean b) {
		return Boolean.TRUE.equals(b);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> extractSpecItemsList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return null;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				out.add((Map<String, Object>) m);
			}
		}
		return out;
	}
}
