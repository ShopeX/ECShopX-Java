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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.order.front.WxappOrderDetailMembercardBundlePort;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryTrackerPullService;
import cn.shopex.ecshopx.orders.service.admin.AdminEntityOrderDetailTypePolicy;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderDeliveryTimelineService;
import cn.shopex.ecshopx.orders.service.logistics.SfbspTrackQueryService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderTrackerPullService {

	private static final DateTimeFormatter ACCEPT_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort;
	private final OrderDeliveryTimelineService orderDeliveryTimelineService;
	private final SfbspTrackQueryService sfbspTrackQueryService;
	private final AdminDeliveryTrackerPullService adminDeliveryTrackerPullService;

	public WxappOrderTrackerPullService(
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			WxappOrderDetailMembercardBundlePort wxappOrderDetailMembercardBundlePort,
			OrderDeliveryTimelineService orderDeliveryTimelineService,
			SfbspTrackQueryService sfbspTrackQueryService,
			AdminDeliveryTrackerPullService adminDeliveryTrackerPullService) {
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.wxappOrderDetailMembercardBundlePort = wxappOrderDetailMembercardBundlePort;
		this.orderDeliveryTimelineService = orderDeliveryTimelineService;
		this.sfbspTrackQueryService = sfbspTrackQueryService;
		this.adminDeliveryTrackerPullService = adminDeliveryTrackerPullService;
	}

	public Object trackerpull(Map<String, Object> auth, String orderTypeRaw, String orderIdRaw) {
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		String normalizedType = normalizeOrderTypeToken(orderTypeRaw);
		if ("supplier_order".equals(normalizedType)) {
			throw new BadRequestException("暂不支持该订单类型的物流查询");
		}
		if (!"membercard".equals(normalizedType)
				&& !AdminEntityOrderDetailTypePolicy.supportsNormalPipelineStatic(normalizedType)) {
			throw new ResourceException("无此类型订单！");
		}

		if (orderIdRaw == null || orderIdRaw.trim().isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		String trimmedOrderId = orderIdRaw.trim();
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(trimmedOrderId);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		Map<String, Object> bundle;
		if ("membercard".equals(normalizedType)) {
			bundle =
					wxappOrderDetailMembercardBundlePort.buildMembercardOrderDetailBundle(
							companyId, String.valueOf(orderIdNum), true);
		} else {
			try {
				bundle =
						adminNormalOrderDetailService.buildOrderBundle(
								companyId, String.valueOf(orderIdNum), true, "front_list");
			} catch (BadRequestException e) {
				String msg = e.getMessage() == null ? "" : e.getMessage();
				if (msg.contains("订单不存在")) {
					throw new ResourceException("订单不存在");
				}
				throw e;
			}
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.get("orderInfo");
		if (!"membercard".equals(normalizedType)) {
			if (orderInfo == null || orderInfo.isEmpty()) {
				throw new ResourceException("订单不存在");
			}
		} else {
			if (orderInfo == null || orderInfo.isEmpty()) {
				orderInfo = new LinkedHashMap<>();
			}
		}

		Object orderUserId = orderInfo.get("user_id");
		String authUserStr = auth.get("user_id") == null ? "" : String.valueOf(auth.get("user_id")).trim();
		String orderUserStr =
				orderUserId == null ? "" : String.valueOf(orderUserId).trim();
		if (!authUserStr.equals(orderUserStr)) {
			return placeholderTraces();
		}

		String receipt =
				orderInfo.get("receipt_type") == null
						? ""
						: String.valueOf(orderInfo.get("receipt_type")).trim();
		if ("merchant".equals(receipt)) {
			return orderDeliveryTimelineService.buildMerchantSelfDeliveryTrackerEntries(companyId, orderIdNum);
		}

		String corpTrimmed =
				orderInfo.get("delivery_corp") == null
						? ""
						: String.valueOf(orderInfo.get("delivery_corp")).trim();
		String codeTrimmed =
				orderInfo.get("delivery_code") == null
						? ""
						: String.valueOf(orderInfo.get("delivery_code")).trim();
		if (corpTrimmed.isEmpty() || codeTrimmed.isEmpty()) {
			return placeholderTraces();
		}

		String receiverMobile =
				orderInfo.get("receiver_mobile") == null
						? ""
						: String.valueOf(orderInfo.get("receiver_mobile")).trim();
		String sourceTrimmed =
				orderInfo.get("delivery_corp_source") == null
						? ""
						: String.valueOf(orderInfo.get("delivery_corp_source")).trim();

		OrdersDelivery synth = new OrdersDelivery();
		synth.setDeliveryCorp(corpTrimmed);
		synth.setDeliveryCode(codeTrimmed);
		synth.setReceiverMobile(receiverMobile);
		synth.setDeliveryCorpSource(sourceTrimmed.isEmpty() ? null : sourceTrimmed);

		try {
			List<LinkedHashMap<String, String>> sf =
					sfbspTrackQueryService.queryTracesIfApplicable(companyId, synth);
			if (sf != null && !sf.isEmpty()) {
				return sf;
			}
			boolean useKuaidi100 =
					orderInfo.get("delivery_corp_source") != null
							&& "kuaidi100"
									.equals(String.valueOf(orderInfo.get("delivery_corp_source")).trim());
			return adminDeliveryTrackerPullService.trackerpull(
					companyId, corpTrimmed, codeTrimmed, useKuaidi100);
		} catch (Exception any) {
			return placeholderTraces();
		}
	}

	private static String normalizeOrderTypeToken(String raw) {
		if (raw == null) {
			return "normal";
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return "normal";
		}
		if ("0".equals(s)) {
			return "normal";
		}
		return s.toLowerCase(Locale.ROOT);
	}

	private static List<LinkedHashMap<String, String>> placeholderTraces() {
		LinkedHashMap<String, String> one = new LinkedHashMap<>();
		one.put("AcceptTime", ACCEPT_TIME_FMT.format(LocalDateTime.now()));
		one.put("AcceptStation", "暂无物流信息");
		return List.of(one);
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
