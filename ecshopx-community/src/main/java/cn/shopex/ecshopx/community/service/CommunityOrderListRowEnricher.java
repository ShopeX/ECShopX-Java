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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.community.domain.CommunityActivity;
import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.mapper.CommunityActivityMapper;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import cn.shopex.ecshopx.distribution.service.DistributorBatchApiRowQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.distribution.service.OrderValidityPlatformSettingReadService;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CommunityOrderListRowEnricher {

	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final CommunityActivityMapper communityActivityMapper;
	private final MemberAccountService memberAccountService;
	private final ItemsMapper itemsMapper;
	private final ObjectMapper objectMapper;
	private final SourcesMapper sourcesMapper;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService;
	private final OperatorsMapper operatorsMapper;
	private final OrdersRelActivityMapper employeePurchaseOrdersRelActivityMapper;

	public CommunityOrderListRowEnricher(
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			CommunityActivityMapper communityActivityMapper,
			MemberAccountService memberAccountService,
			ItemsMapper itemsMapper,
			ObjectMapper objectMapper,
			SourcesMapper sourcesMapper,
			DistributorBatchApiRowQueryService distributorBatchApiRowQueryService,
			DistributorSelfMetaService distributorSelfMetaService,
			OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService,
			OperatorsMapper operatorsMapper,
			OrdersRelActivityMapper employeePurchaseOrdersRelActivityMapper) {
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.communityActivityMapper = communityActivityMapper;
		this.memberAccountService = memberAccountService;
		this.itemsMapper = itemsMapper;
		this.objectMapper = objectMapper;
		this.sourcesMapper = sourcesMapper;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.orderValidityPlatformSettingReadService = orderValidityPlatformSettingReadService;
		this.operatorsMapper = operatorsMapper;
		this.employeePurchaseOrdersRelActivityMapper = employeePurchaseOrdersRelActivityMapper;
	}

	public void enrichForAdminAndFront(long companyId, List<Map<String, Object>> rows) {
		enrichCommunityAndMembers(companyId, rows);
		applyAutoCancelSeconds(rows);
		enrichSourceNames(companyId, rows);
		enrichDistributorRows(companyId, rows);
		enrichSelfDeliveryOperators(companyId, rows);
		enrichOrdersPurchaseInfo(companyId, rows);
		enrichItemSalePrices(companyId, rows);
		applyTotalNum(rows);
	}

	private void applyAutoCancelSeconds(List<Map<String, Object>> rows) {
		long nowSec = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> row : rows) {
			long cancelAtSec = parseAutoCancelSeconds(row.get("auto_cancel_time"));
			row.put("auto_cancel_seconds", cancelAtSec - nowSec);
		}
	}

	private static long parseAutoCancelSeconds(Object raw) {
		if (raw == null) {
			return 0L;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void enrichCommunityAndMembers(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> orderIds =
				rows.stream().map(r -> parseOrderIdLong(r.get("order_id"))).filter(id -> id > 0).distinct().toList();
		List<CommunityOrderRelActivity> rels =
				communityOrderRelActivityMapper.selectList(
						new LambdaQueryWrapper<CommunityOrderRelActivity>()
								.eq(CommunityOrderRelActivity::getCompanyId, companyId)
								.in(CommunityOrderRelActivity::getOrderId, orderIds));
		Map<Long, CommunityOrderRelActivity> relByOrder =
				rels.stream().collect(Collectors.toMap(CommunityOrderRelActivity::getOrderId, r -> r, (a, b) -> a));

		Set<Long> activityIds =
				rels.stream()
						.map(CommunityOrderRelActivity::getActivityId)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		Map<Long, CommunityActivity> actById = new LinkedHashMap<>();
		if (!activityIds.isEmpty()) {
			List<CommunityActivity> actRows =
					communityActivityMapper.selectList(
							new LambdaQueryWrapper<CommunityActivity>()
									.in(CommunityActivity::getActivityId, activityIds));
			for (CommunityActivity a : actRows) {
				actById.put(a.getActivityId(), a);
			}
		}

		List<Long> distinctUserIds =
				rows.stream()
						.map(r -> longVal(r.get("user_id")))
						.filter(uid -> uid > 0)
						.distinct()
						.toList();
		Map<Long, Map<String, Object>> memberByUserId = new LinkedHashMap<>();
		for (Long uid : distinctUserIds) {
			Map<String, Object> mi = memberAccountService.getMemberInfo(uid, companyId);
			memberByUserId.put(uid, mi != null ? mi : new LinkedHashMap<>());
		}

		for (Map<String, Object> row : rows) {
			long oid = parseOrderIdLong(row.get("order_id"));
			CommunityOrderRelActivity rel = relByOrder.get(oid);
			Map<String, Object> communityInfo =
					rel == null ? new LinkedHashMap<>() : communityRelToMap(rel, actById.get(rel.getActivityId()));
			row.put("community_info", communityInfo);

			long userId = longVal(row.get("user_id"));
			if (userId > 0) {
				row.put("member", memberByUserId.getOrDefault(userId, new LinkedHashMap<>()));
			} else {
				row.put("member", new LinkedHashMap<String, Object>());
			}
		}
	}

	private void enrichSourceNames(long companyId, List<Map<String, Object>> rows) {
		List<Long> sourceIds =
				rows.stream()
						.map(r -> longVal(r.get("source_id")))
						.filter(id -> id > 0)
						.distinct()
						.toList();
		Map<Long, String> nameBySourceId = new LinkedHashMap<>();
		if (!sourceIds.isEmpty()) {
			List<Sources> srcRows =
					sourcesMapper.selectList(
							new LambdaQueryWrapper<Sources>()
									.eq(Sources::getCompanyId, companyId)
									.in(Sources::getSourceId, sourceIds));
			for (Sources s : srcRows) {
				if (s.getSourceName() != null) {
					nameBySourceId.put(s.getSourceId(), s.getSourceName());
				}
			}
		}
		for (Map<String, Object> row : rows) {
			long sid = longVal(row.get("source_id"));
			row.put("source_name", sid > 0 ? nameBySourceId.getOrDefault(sid, "-") : "-");
		}
	}

	private void enrichDistributorRows(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		Set<Long> positiveIds = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			long d = longVal(r.get("distributor_id"));
			if (d > 0) {
				positiveIds.add(d);
			}
		}
		Map<Long, Map<String, Object>> byDid =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, positiveIds);
		Map<String, Object> selfTemplate =
				new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
		for (Map<String, Object> row : rows) {
			long did = longVal(row.get("distributor_id"));
			Map<String, Object> info;
			if (did > 0 && byDid.containsKey(did)) {
				info = new LinkedHashMap<>(byDid.get(did));
			} else if (did == 0) {
				info = new LinkedHashMap<>(selfTemplate);
			} else {
				info = new LinkedHashMap<>();
			}
			applyPlatformRefundFreightOnDistributor(companyId, info);
			row.put("distributor_info", info);
			row.put("distributor_name", str(info.get("name")));
		}
	}

	private void applyPlatformRefundFreightOnDistributor(long companyId, Map<String, Object> distributorMap) {
		Map<String, Object> platform = orderValidityPlatformSettingReadService.readOrderValidityPlatformSetting(companyId);
		if (intVal(platform.get("is_refund_freight")) == 0) {
			distributorMap.put("is_refund_freight", 0);
		}
	}

	private void enrichSelfDeliveryOperators(long companyId, List<Map<String, Object>> rows) {
		List<Long> opIds =
				rows.stream()
						.map(r -> longVal(r.get("self_delivery_operator_id")))
						.filter(id -> id > 0)
						.distinct()
						.toList();
		Map<Long, Operators> opById = new LinkedHashMap<>();
		if (!opIds.isEmpty()) {
			List<Operators> ops =
					operatorsMapper.selectList(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, companyId)
									.in(Operators::getOperatorId, opIds));
			for (Operators o : ops) {
				opById.put(o.getOperatorId(), o);
			}
		}
		for (Map<String, Object> row : rows) {
			long oid = longVal(row.get("self_delivery_operator_id"));
			row.put("self_delivery_operator_mobile", "");
			row.put("self_delivery_operator_name", "");
			if (oid > 0) {
				Operators o = opById.get(oid);
				if (o != null) {
					row.put("self_delivery_operator_mobile", o.getMobile() != null ? o.getMobile() : "");
					row.put("self_delivery_operator_name", o.getUsername() != null ? o.getUsername() : "");
				}
			}
		}
	}

	private void enrichOrdersPurchaseInfo(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> orderIds =
				rows.stream()
						.map(r -> parseOrderIdLong(r.get("order_id")))
						.filter(id -> id > 0)
						.distinct()
						.toList();
		Map<Long, OrdersRelActivity> relByOrder = new LinkedHashMap<>();
		if (!orderIds.isEmpty()) {
			List<OrdersRelActivity> rels =
					employeePurchaseOrdersRelActivityMapper.selectList(
							new LambdaQueryWrapper<OrdersRelActivity>()
									.eq(OrdersRelActivity::getCompanyId, companyId)
									.in(OrdersRelActivity::getOrderId, orderIds));
			for (OrdersRelActivity rel : rels) {
				relByOrder.putIfAbsent(rel.getOrderId(), rel);
			}
		}
		for (Map<String, Object> row : rows) {
			long oid = parseOrderIdLong(row.get("order_id"));
			OrdersRelActivity rel = relByOrder.get(oid);
			row.put("orders_purchase_info", rel == null ? null : ordersPurchaseRelToMap(rel));
		}
	}

	private static Map<String, Object> ordersPurchaseRelToMap(OrdersRelActivity rel) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", rel.getOrderId());
		m.put("company_id", rel.getCompanyId());
		m.put("enterprise_id", rel.getEnterpriseId());
		m.put("activity_id", rel.getActivityId());
		m.put("user_id", rel.getUserId());
		m.put("if_share_store", rel.getIfShareStore());
		m.put("close_modify_time", rel.getCloseModifyTime());
		return m;
	}

	private Map<String, Object> communityRelToMap(CommunityOrderRelActivity r, CommunityActivity act) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", r.getOrderId());
		m.put("company_id", r.getCompanyId());
		m.put("chief_id", r.getChiefId());
		m.put("chief_name", r.getChiefName());
		m.put("chief_avatar", r.getChiefAvatar());
		m.put("activity_id", r.getActivityId());
		m.put("activity_name", r.getActivityName());
		m.put("ziti_name", r.getZitiName());
		m.put("ziti_address", r.getZitiAddress());
		m.put("lng", r.getZitiLng());
		m.put("lat", r.getZitiLat());
		m.put("ziti_contact_user", r.getZitiContactUser());
		m.put("ziti_contact_mobile", r.getZitiContactMobile());
		m.put("activity_trade_no", r.getActivityTradeNo());
		m.put("extra_data", parseExtraDataJson(r.getExtraData()));
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		m.put("rebate_ratio", r.getRebateRatio());
		if (act != null) {
			if (StringUtils.hasText(act.getActivityName())) {
				m.put("activity_name", act.getActivityName());
			}
			if (act.getActivityStatus() != null) {
				m.put("activity_status", act.getActivityStatus());
			}
		}
		return m;
	}

	private Object parseExtraDataJson(String raw) {
		if (!StringUtils.hasText(raw)) {
			return raw;
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return raw;
		}
	}

	private void enrichItemSalePrices(long companyId, List<Map<String, Object>> rows) {
		Set<Long> itemIds =
				rows.stream()
						.flatMap(
								r -> {
									Object itemsObj = r.get("items");
									if (!(itemsObj instanceof List<?> list)) {
										return java.util.stream.Stream.<Long>empty();
									}
									List<Long> ids = new ArrayList<>();
									for (Object o : list) {
										if (o instanceof Map<?, ?> im) {
											Object iid = im.get("item_id");
											long v = longVal(iid);
											if (v > 0) {
												ids.add(v);
											}
										}
									}
									return ids.stream();
								})
						.collect(Collectors.toSet());
		if (itemIds.isEmpty()) {
			return;
		}
		List<Items> goods =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getItemId, itemIds));
		Map<Long, Integer> priceByItem = new LinkedHashMap<>();
		for (Items it : goods) {
			if (it.getPrice() != null) {
				priceByItem.put(it.getItemId(), it.getPrice());
			}
		}
		for (Map<String, Object> row : rows) {
			Object itemsObj = row.get("items");
			if (!(itemsObj instanceof List<?> list)) {
				continue;
			}
			for (Object o : list) {
				if (o instanceof Map<?, ?> imRaw) {
					@SuppressWarnings("unchecked")
					Map<String, Object> im = (Map<String, Object>) imRaw;
					long iid = longVal(im.get("item_id"));
					Integer sp = priceByItem.get(iid);
					if (sp != null) {
						im.put("sale_price", sp);
					}
				}
			}
		}
	}

	private static void applyTotalNum(List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			int sum = 0;
			Object itemsObj = row.get("items");
			if (itemsObj instanceof List<?> list) {
				for (Object o : list) {
					if (o instanceof Map<?, ?> im) {
						sum += intVal(im.get("num"));
					}
				}
			}
			row.put("total_num", sum);
		}
	}

	private static long parseOrderIdLong(Object raw) {
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
