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

package cn.shopex.ecshopx.systemlink.service.wdterp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import cn.shopex.ecshopx.common.port.wdterp.dto.WdtInventoryWaitSyncPage;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDeliveryService;
import cn.shopex.ecshopx.promotions.service.WdtErpSyncInventoryPromotionSupport;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class WdtErpSyncService {

	public static final String WDT_REDIS_PREFIX = "WdtErpSetting:";

	private final WdtErpLogisticsPort wdtErpLogisticsPort;
	private final WdtErpInventoryPort wdtErpInventoryPort;
	private final StringRedisTemplate companysRedisTemplate;
	private final DistributorMapper distributorMapper;
	private final DistributorItemsMapper distributorItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final AdminOrderDeliveryService adminOrderDeliveryService;
	private final ObjectMapper objectMapper;
	private final WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport;
	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final ShopMenuService shopMenuService;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public WdtErpSyncService(
			WdtErpLogisticsPort wdtErpLogisticsPort,
			WdtErpInventoryPort wdtErpInventoryPort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			DistributorMapper distributorMapper,
			DistributorItemsMapper distributorItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			AdminOrderDeliveryService adminOrderDeliveryService,
			ObjectMapper objectMapper,
			WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport,
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			ShopMenuService shopMenuService,
			NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.wdtErpLogisticsPort = wdtErpLogisticsPort;
		this.wdtErpInventoryPort = wdtErpInventoryPort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.distributorMapper = distributorMapper;
		this.distributorItemsMapper = distributorItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.adminOrderDeliveryService = adminOrderDeliveryService;
		this.objectMapper = objectMapper;
		this.wdtErpSyncInventoryPromotionSupport = wdtErpSyncInventoryPromotionSupport;
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.shopMenuService = shopMenuService;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	public void scheduleSyncInventory() {
		Set<String> settingKeys = companysRedisTemplate.keys(WDT_REDIS_PREFIX + "*");
		if (settingKeys == null || settingKeys.isEmpty()) {
			return;
		}
		for (String redisKey : settingKeys) {
			Map<String, Object> setting = readSettingJson(redisKey);
			if (!Boolean.TRUE.equals(setting.get("is_open"))) {
				continue;
			}
			syncOneCompanyWdtInventory(setting);
		}
	}

	private void syncOneCompanyWdtInventory(Map<String, Object> setting) {
		long companyId = toLongAny(setting.get("company_id"));
		String sid = asNonNullString(setting.get("sid"));
		String appKey = asNonNullString(setting.get("app_key"));
		String appSecret = asNonNullString(setting.get("app_secret"));
		Set<String> activityBns = wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(companyId);
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		long settingShopId = toLongAny(setting.get("shop_id"));
		int pos = 0;
		while (true) {
			WdtInventoryWaitSyncPage page;
			try {
				page = wdtErpInventoryPort.fetchWaitSyncPage(companyId, pos, sid, appKey, appSecret);
			} catch (Exception e) {
				log.debug("旺店通请求失败:{}", e.getMessage());
				break;
			}
			if (page.recIds().isEmpty()) {
				break;
			}
			for (String recId : page.recIds()) {
				processOneWaitSyncRec(companyId, recId, settingShopId, productModel, activityBns, setting, sid, appKey, appSecret);
			}
			pos = page.nextPosition();
		}
	}

	private void processOneWaitSyncRec(
			long companyId,
			String recId,
			long settingShopId,
			String productModel,
			Set<String> activityBns,
			Map<String, Object> setting,
			String sid,
			String appKey,
			String appSecret) {
		Map<String, Object> stockInfo;
		try {
			stockInfo = wdtErpInventoryPort.queryStore(companyId, recId, sid, appKey, appSecret);
		} catch (Exception e) {
			log.debug("旺店通请求失败:{}", e.getMessage());
			return;
		}
		if (stockInfo == null || stockInfo.isEmpty() || !isUsableWdtInventoryRow(stockInfo)) {
			return;
		}
		String matchCode = String.valueOf(stockInfo.get("match_code")).trim();
		if (!StringUtils.hasText(matchCode) || activityBns.contains(matchCode)) {
			return;
		}
		Items item = itemsRepository.findByItemBnAndCompany(matchCode, companyId);
		if (item == null) {
			wdtErpInventoryPort.acknowledgeSuccess(companyId, recId, stockInfo, sid, appKey, appSecret);
			return;
		}
		long itemId = item.getItemId();
		long freeZ = 0L;
		Long hold = normalOrdersItemsMapper.sumUnpaidHoldingNumForItem(companyId, itemId, System.currentTimeMillis() / 1000L);
		if (hold != null) {
			freeZ = hold;
		}
		int quantity = toBoundedStockQuantity(synStockNumber(stockInfo) - freeZ);
		long stockShop = toLongAny(stockInfo.get("shop_id"));
		boolean shopMatches = settingShopId == stockShop;
		boolean isStandard = "standard".equals(productModel);
		boolean useMainItemStore = shopMatches || !isStandard;
		boolean result;
		if (useMainItemStore) {
			result = runMainStoreBranch(itemId, quantity);
		} else {
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getWdtShopId, stockShop)
									.last("LIMIT 1"));
			if (d == null) {
				wdtErpInventoryPort.acknowledgeSuccess(companyId, recId, stockInfo, sid, appKey, appSecret);
				return;
			}
			result = runDistributorStoreBranch(companyId, d.getDistributorId(), itemId, quantity);
		}
		if (result) {
			wdtErpInventoryPort.acknowledgeSuccess(companyId, recId, stockInfo, sid, appKey, appSecret);
		} else {
			wdtErpInventoryPort.acknowledgeFail(companyId, recId, stockInfo, sid, appKey, appSecret);
		}
	}

	private boolean runMainStoreBranch(long itemId, int quantity) {
		boolean saveOk = itemStoreService.saveItemStoreForWdtSync(itemId, quantity, 0L);
		if (!saveOk) {
			return false;
		}
		itemsRepository.updateSingleItemStoreIfExists(itemId, quantity);
		return true;
	}

	private boolean runDistributorStoreBranch(long companyId, long distributorId, long itemId, int quantity) {
		boolean saveOk = itemStoreService.saveItemStoreForWdtSync(itemId, quantity, distributorId);
		if (!saveOk) {
			return false;
		}
		int n =
				distributorItemsMapper.update(
						null,
						new UpdateWrapper<DistributorItems>()
								.eq("company_id", companyId)
								.eq("distributor_id", distributorId)
								.eq("item_id", itemId)
								.set("store", (long) quantity));
		return n > 0;
	}

	private static long synStockNumber(Map<String, Object> stockInfo) {
		Object v = stockInfo.get("syn_stock");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toBoundedStockQuantity(long v) {
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}

	private static boolean isUsableWdtInventoryRow(Map<String, Object> stockInfo) {
		if (!StringUtils.hasText(String.valueOf(stockInfo.get("match_code")).trim())) {
			return false;
		}
		if (stockInfo.get("shop_id") == null) {
			return false;
		}
		return stockInfo.containsKey("syn_stock") && stockInfo.get("syn_stock") != null;
	}

	public void scheduleSyncLogistics() {
		Set<String> settingKeys = companysRedisTemplate.keys(WDT_REDIS_PREFIX + "*");
		if (settingKeys == null || settingKeys.isEmpty()) {
			return;
		}
		for (String redisKey : settingKeys) {
			Map<String, Object> setting = readSettingJson(redisKey);
			if (!Boolean.TRUE.equals(setting.get("is_open"))) {
				continue;
			}
			long companyId = toLongAny(setting.get("company_id"));
			String sid = asNonNullString(setting.get("sid"));
			String appKey = asNonNullString(setting.get("app_key"));
			String appSecret = asNonNullString(setting.get("app_secret"));
			String settingShopNo = asNonNullString(setting.get("shop_no"));
			for (String shopNo : buildShopNos(companyId, settingShopNo)) {
				int pageNo = 0;
				List<Map<String, Object>> page;
				do {
					page = wdtErpLogisticsPort.getWaitSyncPage(companyId, shopNo, pageNo, sid, appKey, appSecret);
					if (!page.isEmpty()) {
						for (Map<String, Object> row : page) {
							boolean ok = doOrderDelivery(companyId, row);
							row.put("status", ok ? 0 : 2);
						}
						wdtErpLogisticsPort.acknowledgeSync(companyId, page, sid, appKey, appSecret);
					}
					pageNo++;
				} while (!page.isEmpty());
			}
		}
	}

	private List<String> buildShopNos(long companyId, String settingShopNo) {
		List<Distributor> distRows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.gt(Distributor::getWdtShopId, 0L)
								.orderByAsc(Distributor::getWdtShopNo));
		List<String> shopNos = new ArrayList<>();
		for (Distributor d : distRows) {
			String s = d.getWdtShopNo() == null ? null : d.getWdtShopNo().trim();
			if (StringUtils.hasText(s)) {
				shopNos.add(s);
			}
		}
		if (StringUtils.hasText(settingShopNo)) {
			shopNos.add(settingShopNo.trim());
		}
		return shopNos;
	}

	private boolean doOrderDelivery(long companyId, Map<String, Object> logistics) {
		try {
			long tid = toLongAny(logistics.get("tid"));
			OrderAssociations order =
					orderAssociationsMapper.selectOne(
							new LambdaQueryWrapper<OrderAssociations>()
									.eq(OrderAssociations::getCompanyId, companyId)
									.eq(OrderAssociations::getOrderId, tid)
									.last("LIMIT 1"));
			if (order == null) {
				log.debug("订单不存在");
				return false;
			}
			if ("DONE".equals(order.getDeliveryStatus())) {
				log.debug("订单已发货，请勿重复发货");
				return true;
			}
			Map<String, Object> bundle =
					adminNormalOrderDetailService.buildOrderBundle(companyId, String.valueOf(order.getOrderId()), false);
			@SuppressWarnings("unchecked")
			Map<String, Object> orderMap = (Map<String, Object>) bundle.get("orderInfo");
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> itemRows = (List<Map<String, Object>>) orderMap.get("items");
			if (itemRows == null) {
				log.debug("没有发货信息");
			}
			int part = toIntOrZero(logistics.get("is_part_sync"));
			boolean isPartSync = part == 1;
			Set<Long> oidSet = parseOidsToLongSet(String.valueOf(logistics.get("oids") == null ? "" : logistics.get("oids")));

			List<Map<String, Object>> sepInfo = new ArrayList<>();
			if (itemRows != null) {
				for (Map<String, Object> item : itemRows) {
					if ("DONE".equals(String.valueOf(item.get("delivery_status")))) {
						continue;
					}
					if (!"PENDING".equals(String.valueOf(item.get("delivery_status")))) {
						continue;
					}
					Map<String, Object> copy = new LinkedHashMap<>(item);
					copy.put("delivery_code", asNonNullString(logistics.get("logistics_no")));
					copy.put("delivery_corp", asNonNullString(logistics.get("logistics_code")));
					copy.put("delivery_num", item.get("num"));
					if (isPartSync) {
						long lineId = toLongAny(item.get("id"));
						if (oidSet.contains(lineId)) {
							sepInfo.add(copy);
						}
					} else {
						sepInfo.add(copy);
					}
				}
			}
			if (sepInfo.isEmpty()) {
				log.debug("没有发货信息");
			}
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("type", "new");
			params.put("company_id", toLongAny(orderMap.get("company_id")));
			params.put("delivery_code", asNonNullString(logistics.get("logistics_no")));
			params.put("delivery_corp", asNonNullString(logistics.get("logistics_code")));
			params.put("delivery_type", "sep");
			params.put("order_id", toLongAny(orderMap.get("order_id")));
			try {
				params.put("sepInfo", objectMapper.writeValueAsString(sepInfo));
			} catch (JsonProcessingException e) {
				log.debug("旺店通 发货失败:{}", e.getMessage());
				return false;
			}
			log.debug("旺店通 去发货 delivery_params=>{}", params);
			try {
				Map<String, Object> r = adminOrderDeliveryService.delivery(companyId, "system", 0L, params);
				return r != null;
			} catch (ResourceException e) {
				log.debug("旺店通 发货失败", e);
				return false;
			} catch (Exception e) {
				log.debug("旺店通 发货失败", e);
				return false;
			}
		} catch (Exception e) {
			log.debug("旺店通 发货失败", e);
			return false;
		}
	}

	private static Set<Long> parseOidsToLongSet(String raw) {
		Set<Long> out = new HashSet<>();
		if (!StringUtils.hasText(raw)) {
			return out;
		}
		for (String p : raw.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static int toIntOrZero(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLongAny(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String asNonNullString(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private Map<String, Object> readSettingJson(String redisKey) {
		String raw = companysRedisTemplate.opsForValue().get(redisKey);
		if (!StringUtils.hasText(raw)) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("is_open", Boolean.FALSE);
			return m;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> p = objectMapper.readValue(raw, Map.class);
			return p == null ? Map.of("is_open", false) : p;
		} catch (Exception e) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("is_open", Boolean.FALSE);
			return m;
		}
	}
}
