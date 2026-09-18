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

package cn.shopex.ecshopx.community.service.admin;

import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.distribution.service.OrderValidityPlatformSettingReadService;
import cn.shopex.ecshopx.espier.domain.Subdistrict;
import cn.shopex.ecshopx.espier.mapper.SubdistrictMapper;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderDeliveryTimelineService;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommunityAdminOrderDetailService {

	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final OrderProfitMapper orderProfitMapper;
	private final MemberAccountService memberAccountService;
	private final OrderDeliveryTimelineService orderDeliveryTimelineService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SubdistrictMapper subdistrictMapper;
	private final CommunityAdminOrderDatapassUiApplier communityAdminOrderDatapassUiApplier;

	public CommunityAdminOrderDetailService(
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			OrderProfitMapper orderProfitMapper,
			MemberAccountService memberAccountService,
			OrderDeliveryTimelineService orderDeliveryTimelineService,
			DistributorSelfMetaService distributorSelfMetaService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService,
			ShopSalespersonMapper shopSalespersonMapper,
			SubdistrictMapper subdistrictMapper,
			CommunityAdminOrderDatapassUiApplier communityAdminOrderDatapassUiApplier) {
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.orderProfitMapper = orderProfitMapper;
		this.memberAccountService = memberAccountService;
		this.orderDeliveryTimelineService = orderDeliveryTimelineService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.orderValidityPlatformSettingReadService = orderValidityPlatformSettingReadService;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.subdistrictMapper = subdistrictMapper;
		this.communityAdminOrderDatapassUiApplier = communityAdminOrderDatapassUiApplier;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getDetail(long companyId, String orderId, HttpServletRequest request) {
		Map<String, Object> bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderId, false);
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		Map<String, Object> tradeInfo = (Map<String, Object>) bundle.get("tradeInfo");

		Map<String, Object> distributorMap = resolveDistributorMap(companyId, orderInfo);
		applyPlatformRefundFreightOnDistributor(companyId, distributorMap);
		orderInfo.put("distributor_name", str(distributorMap.get("name")));
		orderInfo.put("distributor_info", distributorMap);
		bundle.put("distributor", distributorMap);

		applySubdistrictLabels(orderInfo);

		CommunityOrderRelActivity rel =
				communityOrderRelActivityMapper.selectById(parseOrderIdLong(orderId));
		orderInfo.put("community_info", rel == null ? new LinkedHashMap<String, Object>() : communityRelToMap(rel));

		bundle.put("community_activity", null);

		bundle.put("profit", buildProfitPayload(companyId, orderId));

		long userId = longVal(orderInfo.get("user_id"));
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		orderInfo.put("user_delete", memberInfo == null || memberInfo.isEmpty());

		applyLatestAftersaleTime(orderInfo, companyId);

		Object appInfoObj = orderInfo.get("app_info");
		if (appInfoObj instanceof Map<?, ?> appInfoRaw && !appInfoRaw.isEmpty()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> appInfo = (Map<String, Object>) appInfoRaw;
			List<Map<String, Object>> deliveryLog =
					orderDeliveryTimelineService.buildDeliveryLog(orderInfo, tradeInfo, dadaMap(orderInfo));
			appInfo.put("delivery_log", deliveryLog);
		}

		if (DatapassBlockResolver.isBlocked(request)) {
			communityAdminOrderDatapassUiApplier.applyBlockedAppInfoAndOrderMask(orderInfo);
		}

		return bundle;
	}

	private static Map<String, Object> dadaMap(Map<String, Object> orderInfo) {
		Object d = orderInfo.get("dada");
		if (d instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dm = (Map<String, Object>) m;
			return dm;
		}
		return new LinkedHashMap<>();
	}

	private Map<String, Object> resolveDistributorMap(long companyId, Map<String, Object> orderInfo) {
		long distributorId = longVal(orderInfo.get("distributor_id"));
		if (distributorId > 0) {
			Object row = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(distributorId));
			if (row instanceof Map<?, ?> m) {
				return new LinkedHashMap<String, Object>((Map<String, Object>) m);
			}
		}
		return new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
	}

	private void applyPlatformRefundFreightOnDistributor(long companyId, Map<String, Object> distributorMap) {
		Map<String, Object> platform = orderValidityPlatformSettingReadService.readOrderValidityPlatformSetting(companyId);
		if (intVal(platform.get("is_refund_freight")) == 0) {
			distributorMap.put("is_refund_freight", 0);
		}
	}

	private void applySubdistrictLabels(Map<String, Object> orderInfo) {
		Long parentId = longOrNull(orderInfo.get("subdistrict_parent_id"));
		if (parentId != null) {
			Subdistrict s = subdistrictMapper.selectById(parentId);
			orderInfo.put("subdistrict_parent", s != null && s.getLabel() != null ? s.getLabel() : "");
		}
		Long sid = longOrNull(orderInfo.get("subdistrict_id"));
		if (sid != null) {
			Subdistrict s = subdistrictMapper.selectById(sid);
			orderInfo.put("subdistrict", s != null && s.getLabel() != null ? s.getLabel() : "");
		}
	}

	private void applyLatestAftersaleTime(Map<String, Object> orderInfo, long companyId) {
		if (!"DONE".equals(str(orderInfo.get("order_status")))) {
			orderInfo.put("latest_aftersale_time", 0);
			return;
		}
		int endTime = intVal(orderInfo.get("end_time"));
		Map<String, Object> platform = orderValidityPlatformSettingReadService.readOrderValidityPlatformSetting(companyId);
		int last = intVal(platform.get("latest_aftersale_time"));
		if (last <= 0 || endTime <= 0) {
			orderInfo.put("latest_aftersale_time", -1);
			return;
		}
		long endSec = endTime > 9_999_999_999L ? endTime / 1000L : endTime;
		ZoneId z = ZoneId.systemDefault();
		LocalDate endDate = Instant.ofEpochSecond(endSec).atZone(z).toLocalDate();
		ZonedDateTime boundary = endDate.plusDays(last + 1L).atStartOfDay(z);
		long boundaryLastSec = boundary.toEpochSecond() - 1;
		long now = Instant.now().getEpochSecond();
		orderInfo.put("latest_aftersale_time", (int) (boundaryLastSec - now));
	}

	private Object buildProfitPayload(long companyId, String orderId) {
		long oid = parseOrderIdLong(orderId);
		OrderProfit p =
				orderProfitMapper.selectOne(
						new LambdaQueryWrapper<OrderProfit>().eq(OrderProfit::getOrderId, oid).last("LIMIT 1"));
		if (p == null) {
			return new ArrayList<>();
		}
		Map<String, Object> m = profitToMap(p);
		Long distId = p.getDistributorId();
		if (distId != null && distId > 0) {
			Object row = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, String.valueOf(distId));
			if (row instanceof Map<?, ?> dm) {
				m.put("distributor_info", new LinkedHashMap<String, Object>((Map<String, Object>) dm));
			} else {
				m.put("distributor_info", new LinkedHashMap<String, Object>());
			}
		} else {
			m.put("distributor_info", new LinkedHashMap<String, Object>());
		}
		m.put("seller_info", loadSalesperson(companyId, p.getSellerId()));
		m.put("popularize_seller_info", loadSalesperson(companyId, p.getPopularizeSellerId()));
		List<Map<String, Object>> asList = new ArrayList<>();
		asList.add(m);
		return asList;
	}

	private Map<String, Object> loadSalesperson(long companyId, Long salespersonId) {
		if (salespersonId == null || salespersonId <= 0) {
			return new LinkedHashMap<>();
		}
		ShopSalesperson sp =
				shopSalespersonMapper.selectOne(
						new LambdaQueryWrapper<ShopSalesperson>()
								.eq(ShopSalesperson::getCompanyId, companyId)
								.eq(ShopSalesperson::getSalespersonId, salespersonId)
								.last("LIMIT 1"));
		if (sp == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("salesperson_id", sp.getSalespersonId());
		m.put("name", sp.getName());
		m.put("mobile", sp.getMobile());
		m.put("number", sp.getNumber());
		m.put("shop_name", sp.getShopName());
		return m;
	}

	private static Map<String, Object> profitToMap(OrderProfit p) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", p.getId());
		m.put("order_id", p.getOrderId());
		m.put("order_profit_status", p.getOrderProfitStatus());
		m.put("company_id", p.getCompanyId());
		m.put("total_fee", p.getTotalFee());
		m.put("pay_fee", p.getPayFee());
		m.put("profit_type", p.getProfitType());
		m.put("user_id", p.getUserId());
		m.put("dealer_id", p.getDealerId());
		m.put("distributor_id", p.getDistributorId());
		m.put("order_distributor_id", p.getOrderDistributorId());
		m.put("distributor_nid", p.getDistributorNid());
		m.put("seller_id", p.getSellerId());
		m.put("popularize_distributor_id", p.getPopularizeDistributorId());
		m.put("popularize_seller_id", p.getPopularizeSellerId());
		m.put("proprietary", p.getProprietary());
		m.put("popularize_proprietary", p.getPopularizeProprietary());
		m.put("dealers", p.getDealers());
		m.put("distributor", p.getDistributor());
		m.put("seller", p.getSeller());
		m.put("popularize_distributor", p.getPopularizeDistributor());
		m.put("popularize_seller", p.getPopularizeSeller());
		m.put("commission", p.getCommission());
		m.put("rule", p.getRule());
		m.put("plan_close_time", p.getPlanCloseTime());
		m.put("created", p.getCreated());
		m.put("updated", p.getUpdated());
		return m;
	}

	private static Map<String, Object> communityRelToMap(CommunityOrderRelActivity r) {
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
		m.put("extra_data", r.getExtraData());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		m.put("rebate_ratio", r.getRebateRatio());
		return m;
	}

	private static long parseOrderIdLong(String orderId) {
		try {
			return Long.parseLong(orderId.trim());
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

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v == 0L ? null : v;
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
