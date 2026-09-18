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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.common.dispatch.AddDistributorItemsJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformProductSyncPort;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistributorItemsPagedAddRunner {

	private final AddDistributorItemsJobDispatchPublisher addDistributorItemsJobDispatchPublisher;
	private final DistributorItemsSkuPageQueryService skuPageQuery;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ShuyunOpenPlatformProductSyncPort openPlatformProductSyncPort;

	public DistributorItemsPagedAddRunner(
			AddDistributorItemsJobDispatchPublisher addDistributorItemsJobDispatchPublisher,
			DistributorItemsSkuPageQueryService skuPageQuery,
			DistributorItemsRepository distributorItemsRepository,
			ShuyunOpenPlatformProductSyncPort openPlatformProductSyncPort) {
		this.addDistributorItemsJobDispatchPublisher = addDistributorItemsJobDispatchPublisher;
		this.skuPageQuery = skuPageQuery;
		this.distributorItemsRepository = distributorItemsRepository;
		this.openPlatformProductSyncPort = openPlatformProductSyncPort;
	}

	public boolean runAllPagesSync(AddJobParams params) {
		int pageSize = params.pageSize();
		long total = skuPageQuery.countSkus(params.companyId(), params.defaultItemIdInFilter());
		if (total == 0) {
			return true;
		}
		int page = 1;
		while ((long) (page - 1) * pageSize < total) {
			List<Items> rows = skuPageQuery.selectPage(
					params.companyId(), params.defaultItemIdInFilter(), page, pageSize);
			relDistributorItem(params.companyId(), params.distributorId(), rows, params.isCanSale());
			if ((long) page * pageSize >= total) {
				break;
			}
			page++;
		}
		return true;
	}

	public void enqueueAsyncFirstPage(AddJobParams params) {
		addDistributorItemsJobDispatchPublisher.enqueueFirstPage(
				params.companyId(),
				params.distributorId(),
				params.defaultItemIdInFilter(),
				params.isCanSale(),
				params.pageSize());
	}

	public void consumeOneQueuedPage(Map<String, Object> payload) {
		try {
			AddJobParams params = parsePayload(payload);
			int pageSize = params.pageSize();
			long total = skuPageQuery.countSkus(params.companyId(), params.defaultItemIdInFilter());
			if (params.page() == 1 && total == 0) {
				return;
			}
			List<Items> rows = skuPageQuery.selectPage(
					params.companyId(), params.defaultItemIdInFilter(), params.page(), pageSize);
			if (rows.isEmpty()) {
				return;
			}
			relDistributorItem(params.companyId(), params.distributorId(), rows, params.isCanSale());
			if ((long) params.page() * pageSize < total) {
				addDistributorItemsJobDispatchPublisher.enqueuePage(
						params.companyId(),
						params.distributorId(),
						params.defaultItemIdInFilter(),
						params.isCanSale(),
						params.page() + 1,
						pageSize);
			}
		} catch (Exception e) {
			log.error(
					"distributor items async page failed companyId={} distributorId={} page in payload",
					payload != null ? payload.get("company_id") : null,
					payload != null ? payload.get("distributor_id") : null,
					e);
		}
	}

	private static AddJobParams parsePayload(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		long distributorId = toLong(payload.get("distributor_id"));
		List<Long> itemFilter = parseItemIdsFilter(payload.get("item_ids"));
		boolean isCanSale = parseBoolean(payload.get("is_can_sale"));
		int page = toInt(payload.get("page"), 1);
		Object pageSizeObj = payload.containsKey("pageSize") ? payload.get("pageSize") : payload.get("page_size");
		int pageSize = toInt(pageSizeObj, 100);
		return new AddJobParams(companyId, distributorId, itemFilter, isCanSale, page, pageSize);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static int toInt(Object o, int defaultValue) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString());
	}

	private static boolean parseBoolean(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(o.toString());
	}

	@SuppressWarnings("unchecked")
	private static List<Long> parseItemIdsFilter(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			if ("_all".equals(s)) {
				return null;
			}
			throw new BadRequestException("item_ids 格式错误：字符串仅支持 _all");
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>(list.size());
			for (Object el : list) {
				if (el instanceof Number n) {
					out.add(n.longValue());
				} else {
					out.add(Long.parseLong(el.toString()));
				}
			}
			return out;
		}
		throw new BadRequestException("item_ids 格式错误：须为商品 ID 列表或 _all");
	}

	private void relDistributorItem(long companyId, long distributorId, List<Items> rows, boolean isCanSale) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> pageSkuIds = new ArrayList<>();
		for (Items it : rows) {
			if (it.getItemId() != null) {
				pageSkuIds.add(it.getItemId());
			}
		}
		if (pageSkuIds.isEmpty()) {
			return;
		}
		List<DistributorItems> existing =
				distributorItemsRepository.listByDistributorAndItemIds(companyId, distributorId, pageSkuIds);
		Map<Long, DistributorItems> byItemId = new HashMap<>();
		for (DistributorItems di : existing) {
			if (di.getItemId() != null) {
				byItemId.put(di.getItemId(), di);
			}
		}
		Set<Long> touchedGoodsIds = new HashSet<>();
		Set<Long> syncDefaultItemIds = new LinkedHashSet<>();
		for (Items item : rows) {
			if (item.getItemId() == null) {
				continue;
			}
			long itemId = item.getItemId();
			Long def = item.getDefaultItemId();
			long defaultItemId = (def != null && def > 0) ? def : itemId;
			Long goodsId = item.getGoodsId() != null ? item.getGoodsId() : 0L;
			boolean isShow = defaultItemId == itemId;
			Integer sup = item.getSupplierId();
			boolean isTotalStore = sup != null && sup > 0;
			long price = item.getPrice() != null ? item.getPrice().longValue() : 0L;
			DistributorItems rowEx = byItemId.get(itemId);
			if (rowEx == null) {
				DistributorItems ins = new DistributorItems();
				ins.setCompanyId(companyId);
				ins.setDistributorId(distributorId);
				ins.setItemId(itemId);
				ins.setGoodsId(goodsId);
				ins.setPrice(price);
				ins.setShopId(0L);
				ins.setStore(0L);
				ins.setIsTotalStore(isTotalStore);
				ins.setDefaultItemId(defaultItemId);
				ins.setIsShow(isShow);
				ins.setIsCanSale(isCanSale);
				ins.setIsSelfDelivery(false);
				ins.setIsExpressDelivery(false);
				distributorItemsRepository.insert(ins);
				if (goodsId != null && goodsId > 0) {
					touchedGoodsIds.add(goodsId);
				}
			} else {
				distributorItemsRepository.updateColumnsByItemIdAndDistributor(
						companyId, distributorId, itemId, defaultItemId, goodsId, isShow);
				if (goodsId != null && goodsId > 0) {
					touchedGoodsIds.add(goodsId);
				}
			}
			syncDefaultItemIds.add(defaultItemId);
		}
		for (Long gid : touchedGoodsIds) {
			distributorItemsRepository.applyGoodsCanSaleSync(distributorId, gid);
		}
		for (Long defId : syncDefaultItemIds) {
			if (defId != null && defId > 0) {
				openPlatformProductSyncPort.dispatchIfAuthAllows(companyId, distributorId, defId);
			}
		}
	}

	public record AddJobParams(
			long companyId,
			long distributorId,
			List<Long> defaultItemIdInFilter,
			boolean isCanSale,
			int page,
			int pageSize) {
		AddJobParams nextPage() {
			return new AddJobParams(companyId, distributorId, defaultItemIdInFilter, isCanSale, page + 1, pageSize);
		}
	}
}
