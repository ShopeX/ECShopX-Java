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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsPriceStoreStatusPatch;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsUpdatePatch;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsPriceStoreStatusWriteService {

	private final SupplierItemsAdminBatchUpdateService supplierItemsAdminBatchUpdateService;
	private final ItemsRepository itemsRepository;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;
	private final ItemsTableLangSyncAfterMainUpdateService itemsTableLangSyncAfterMainUpdateService;

	public ItemsPriceStoreStatusWriteService(
			SupplierItemsAdminBatchUpdateService supplierItemsAdminBatchUpdateService,
			ItemsRepository itemsRepository,
			ItemCreatePromotionGuardService itemCreatePromotionGuardService,
			ItemsTableLangSyncAfterMainUpdateService itemsTableLangSyncAfterMainUpdateService) {
		this.supplierItemsAdminBatchUpdateService = supplierItemsAdminBatchUpdateService;
		this.itemsRepository = itemsRepository;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
		this.itemsTableLangSyncAfterMainUpdateService = itemsTableLangSyncAfterMainUpdateService;
	}

	public void updateFromRequest(long companyId, String operatorTypeJwt, Map<String, Object> merged) {
		PlatformUpdatePlan plan = buildPlatformPlan(merged);
		if (plan.emptyAfterBuild) {
			throw new BadRequestException("params 错误");
		}
		Location loc = resolveLocation(merged);
		if ("supplier".equals(operatorTypeJwt)) {
			SupplierItemsAdminBatchUpdateService.SupplierItemsFilter filter =
					new SupplierItemsAdminBatchUpdateService.SupplierItemsFilter();
			if (loc.goodsIds != null) {
				filter.setGoodsIdsOrNull(loc.goodsIds);
				filter.setItemIdsOrNull(null);
			} else {
				filter.setItemIdsOrNull(loc.itemIds);
				filter.setGoodsIdsOrNull(null);
			}
			if (plan.auditStatusFilterProcessing) {
				filter.setAuditStatusInOrNull(List.of("submitting", "rejected"));
			}
			SupplierItemsUpdatePatch patch = toSupplierPatch(plan);
			supplierItemsAdminBatchUpdateService.batchUpdateItems(companyId, filter, patch);
			return;
		}
		Long goodsId = loc.goodsIds != null && !loc.goodsIds.isEmpty() ? loc.goodsIds.get(0) : null;
		Long itemId = loc.itemIds != null && !loc.itemIds.isEmpty() ? loc.itemIds.get(0) : null;
		if (plan.priceFenOrNull != null) {
			Items row = itemsRepository.selectOneForPriceCheckAndPromotion(companyId, itemId, goodsId);
			if (row == null) {
				throw new ResourceException("商品不存在");
			}
			long priceFenLong = plan.priceFenOrNull.longValue();
			if (priceFenLong <= 0 && !Boolean.TRUE.equals(row.getIsGift())) {
				throw new ResourceException("非赠品价格须为正");
			}
			Map<Long, Long> priceMap = new LinkedHashMap<>();
			priceMap.put(row.getItemId(), priceFenLong);
			priceMap.put(row.getGoodsId(), priceFenLong);
			itemCreatePromotionGuardService.checkItemPrice(companyId, List.of(row.getGoodsId()), priceMap);
		}
		List<String> auditIn = plan.auditStatusFilterProcessing ? List.of("submitting", "rejected") : null;
		ItemsPriceStoreStatusPatch platformPatch = toItemsPatch(plan);
		itemsRepository.updateByItemsPriceStoreFilter(companyId, itemId, goodsId, auditIn, platformPatch);
		if (itemId != null) {
			itemsTableLangSyncAfterMainUpdateService.afterItemsUpdateIfLangFieldsPresent(
					companyId, itemId, merged);
		}
	}

	private static final class Location {
		private final List<Long> goodsIds;
		private final List<Long> itemIds;

		private Location(List<Long> goodsIds, List<Long> itemIds) {
			this.goodsIds = goodsIds;
			this.itemIds = itemIds;
		}
	}

	private static Location resolveLocation(Map<String, Object> merged) {
		if (merged.containsKey("goods_id") && merged.get("goods_id") != null) {
			List<Long> ids = parsePositiveLongList(merged.get("goods_id"));
			if (!ids.isEmpty()) {
				return new Location(ids, null);
			}
		}
		if (merged.containsKey("item_id") && merged.get("item_id") != null) {
			List<Long> ids = parsePositiveLongList(merged.get("item_id"));
			if (!ids.isEmpty()) {
				return new Location(null, ids);
			}
		}
		throw new BadRequestException("请指定商品");
	}

	private static List<Long> parsePositiveLongList(Object raw) {
		if (isIdSequence(raw)) {
			List<Long> ids = new ArrayList<>();
			for (Long id : toLongListFromSequence(raw)) {
				if (id <= 0) {
					throw new BadRequestException("请指定商品");
				}
				ids.add(id);
			}
			return ids;
		}
		if (!StringUtils.hasText(String.valueOf(raw).trim())) {
			return List.of();
		}
		return List.of(parsePositiveLong(raw));
	}

	private static boolean isIdSequence(Object raw) {
		return raw instanceof Collection<?>
				|| raw instanceof Object[]
				|| raw instanceof long[]
				|| raw instanceof int[]
				|| raw instanceof short[]
				|| raw instanceof byte[]
				|| raw instanceof Integer[]
				|| raw instanceof Long[];
	}

	private static List<Long> toLongListFromSequence(Object raw) {
		if (raw instanceof Collection<?> c) {
			List<Long> ids = new ArrayList<>();
			for (Object o : c) {
				if (o != null) {
					ids.add(parseLongElement(o));
				}
			}
			return ids;
		}
		if (raw instanceof Object[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Object o : arr) {
				if (o != null) {
					ids.add(parseLongElement(o));
				}
			}
			return ids;
		}
		if (raw instanceof long[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (long v : arr) {
				ids.add(v);
			}
			return ids;
		}
		if (raw instanceof int[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (int v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof short[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (short v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof byte[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (byte v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof Integer[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Integer o : arr) {
				if (o != null) {
					ids.add(o.longValue());
				}
			}
			return ids;
		}
		if (raw instanceof Long[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Long o : arr) {
				if (o != null) {
					ids.add(o);
				}
			}
			return ids;
		}
		return List.of();
	}

	private static long parseLongElement(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("请指定商品");
		}
	}

	private static long parsePositiveLong(Object raw) {
		long v;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(String.valueOf(raw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("请指定商品");
			}
		}
		if (v <= 0) {
			throw new BadRequestException("请指定商品");
		}
		return v;
	}

	private static final class PlatformUpdatePlan {
		private Integer priceFenOrNull;
		private Integer costPriceFenOrNull;
		private Integer marketPriceFenOrNull;
		private Integer storeOrNull;
		private Integer rebateOrNull;
		private String rebateTypeOrNull;
		private String approveStatusOrNull;
		private Integer isMarketOrNull;
		private boolean setIsMarketColumn;
		private Object isMarketRawOrNull;
		private String auditStatusOrNull;
		private boolean auditStatusFilterProcessing;
		private boolean emptyAfterBuild;
	}

	private static PlatformUpdatePlan buildPlatformPlan(Map<String, Object> merged) {
		PlatformUpdatePlan p = new PlatformUpdatePlan();
		int count = 0;
		if (merged.containsKey("price") && merged.get("price") != null) {
			p.priceFenOrNull = parsePriceToFen(merged.get("price"));
			count++;
		}
		if (merged.containsKey("cost_price") && merged.get("cost_price") != null) {
			Integer cp = parseMoneyFieldToFenIfNonZero(merged.get("cost_price"));
			if (cp != null) {
				p.costPriceFenOrNull = cp;
				count++;
			}
		}
		if (merged.containsKey("market_price") && merged.get("market_price") != null) {
			Integer mp = parseMoneyFieldToFenIfNonZero(merged.get("market_price"));
			if (mp != null) {
				p.marketPriceFenOrNull = mp;
				count++;
			}
		}
		if (merged.containsKey("store") && merged.get("store") != null) {
			p.storeOrNull = parseIntField(merged.get("store"));
			count++;
		}
		if (merged.containsKey("rebate") && merged.get("rebate") != null) {
			p.rebateOrNull = parseIntField(merged.get("rebate"));
			count++;
		}
		if (merged.containsKey("rebate_type") && merged.get("rebate_type") != null) {
			p.rebateTypeOrNull = merged.get("rebate_type").toString();
			count++;
		}
		if (merged.containsKey("status") && merged.get("status") != null) {
			p.approveStatusOrNull = merged.get("status").toString();
			count++;
		}
		if (merged.containsKey("is_market") && merged.get("is_market") != null) {
			p.setIsMarketColumn = true;
			p.isMarketRawOrNull = merged.get("is_market");
			p.isMarketOrNull = normalizeIsMarket(merged.get("is_market"));
			count++;
		}
		if (merged.containsKey("audit_status") && merged.get("audit_status") != null) {
			String as = merged.get("audit_status").toString();
			p.auditStatusOrNull = as;
			if ("processing".equals(as)) {
				p.auditStatusFilterProcessing = true;
			}
			count++;
		}
		p.emptyAfterBuild = count == 0;
		return p;
	}

	private static int parseIntField(Object v) {
		if (v instanceof Number n) {
			try {
				return Math.toIntExact(n.longValue());
			} catch (ArithmeticException e) {
				throw new BadRequestException("params 错误");
			}
		}
		try {
			return Math.toIntExact(Long.parseLong(String.valueOf(v).trim()));
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException("params 错误");
		}
	}

	private static Integer parseMoneyFieldToFenIfNonZero(Object v) {
		BigDecimal bd = new BigDecimal(String.valueOf(v).trim());
		if (bd.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		try {
			return bd.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
		} catch (ArithmeticException e) {
			throw new BadRequestException("价格格式错误");
		}
	}

	private static int parsePriceToFen(Object v) {
		String trim = String.valueOf(v).trim();
		BigDecimal bd;
		try {
			bd = new BigDecimal(trim);
		} catch (NumberFormatException e) {
			throw new BadRequestException("价格格式错误");
		}
		try {
			BigDecimal fen = bd.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
			long lv = fen.longValueExact();
			return Math.toIntExact(lv);
		} catch (ArithmeticException e) {
			throw new BadRequestException("价格格式错误");
		}
	}

	private static int normalizeIsMarket(Object raw) {
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0 ? 1 : 0;
		}
		String s = String.valueOf(raw).trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
			return 1;
		}
		return 0;
	}

	private static ItemsPriceStoreStatusPatch toItemsPatch(PlatformUpdatePlan p) {
		return new ItemsPriceStoreStatusPatch(
				p.priceFenOrNull,
				p.costPriceFenOrNull,
				p.marketPriceFenOrNull,
				p.storeOrNull,
				p.rebateOrNull,
				p.rebateTypeOrNull,
				p.approveStatusOrNull,
				p.setIsMarketColumn ? p.isMarketOrNull : null,
				p.auditStatusOrNull);
	}

	private static SupplierItemsUpdatePatch toSupplierPatch(PlatformUpdatePlan p) {
		SupplierItemsUpdatePatch patch = new SupplierItemsUpdatePatch();
		patch.setPrice(p.priceFenOrNull);
		patch.setCostPrice(p.costPriceFenOrNull);
		patch.setMarketPrice(p.marketPriceFenOrNull);
		patch.setStore(p.storeOrNull);
		patch.setRebate(p.rebateOrNull);
		patch.setRebateType(p.rebateTypeOrNull);
		patch.setApproveStatus(p.approveStatusOrNull);
		patch.setAuditStatus(p.auditStatusOrNull);
		if (p.setIsMarketColumn) {
			patch.setMarketColumnPresent(true);
			patch.setIsMarketValueRaw(p.isMarketRawOrNull);
			patch.setIsMarket(p.isMarketOrNull);
		}
		return patch;
	}
}
