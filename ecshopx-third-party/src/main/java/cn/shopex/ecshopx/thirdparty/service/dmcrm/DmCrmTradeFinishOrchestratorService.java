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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.common.port.distribution.DmCrmTradeFinishGuideDistributorReadPort;
import cn.shopex.ecshopx.common.port.goods.GoodsItemsListsByItemIdsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.common.port.salesperson.ShoppingGuideSalespersonRowReadPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DmCrmTradeFinishOrchestratorService {

	private static final Logger log = LoggerFactory.getLogger(DmCrmTradeFinishOrchestratorService.class);

	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final GoodsItemsListsByItemIdsReadPort goodsItemsListsByItemIdsReadPort;
	private final ShoppingGuideSalespersonRowReadPort shoppingGuideSalespersonRowReadPort;
	private final DmCrmTradeFinishGuideDistributorReadPort dmCrmTradeFinishGuideDistributorReadPort;
	private final PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort;
	private final DmCrmManualPointChangePort dmCrmManualPointChangePort;
	private final DmCrmTradeFinishOrderSyncPort dmCrmTradeFinishOrderSyncPort;

	public DmCrmTradeFinishOrchestratorService(
			DmCrmSettingReadPort dmCrmSettingReadPort,
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			GoodsItemsListsByItemIdsReadPort goodsItemsListsByItemIdsReadPort,
			ShoppingGuideSalespersonRowReadPort shoppingGuideSalespersonRowReadPort,
			DmCrmTradeFinishGuideDistributorReadPort dmCrmTradeFinishGuideDistributorReadPort,
			PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort,
			DmCrmManualPointChangePort dmCrmManualPointChangePort,
			DmCrmTradeFinishOrderSyncPort dmCrmTradeFinishOrderSyncPort) {
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.goodsItemsListsByItemIdsReadPort = goodsItemsListsByItemIdsReadPort;
		this.shoppingGuideSalespersonRowReadPort = shoppingGuideSalespersonRowReadPort;
		this.dmCrmTradeFinishGuideDistributorReadPort = dmCrmTradeFinishGuideDistributorReadPort;
		this.pointMemberDmPointMemberInfoReadPort = pointMemberDmPointMemberInfoReadPort;
		this.dmCrmManualPointChangePort = dmCrmManualPointChangePort;
		this.dmCrmTradeFinishOrderSyncPort = dmCrmTradeFinishOrderSyncPort;
	}

	public void handleTradeFinish(Map<String, Object> tradeRowPayload) {
		try {
			long companyId = requireCompanyId(tradeRowPayload);
			long orderId = requireOrderId(tradeRowPayload);
			handlePoint(companyId, orderId);
			handleSync(companyId, orderId);
		} catch (Exception e) {
			log.debug("trade_finish orchestrator swallowed: {}", e.toString());
		}
	}

	private void handlePoint(long companyId, long orderId) {
		try {
			if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
				return;
			}
			Optional<Map<String, Object>> headerOpt =
					orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
			if (headerOpt.isEmpty()) {
				return;
			}
			Map<String, Object> orderInfo = headerOpt.get();
			if (shouldSkipPointAndSyncGuards(orderInfo)) {
				return;
			}
			String preId = String.valueOf(orderInfo.getOrDefault("dm_point_preid", "")).trim();
			if (!StringUtils.hasText(preId)) {
				return;
			}
			long userId = toPrimitiveLong(orderInfo.get("user_id"));
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			String mobile = stringOrEmpty(memberInfo.get("mobile"));
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(0)
							.type(0)
							.changeType("")
							.remark("")
							.integralFlow("")
							.sourceChannel("c_brand_mall")
							.preDeductionId(preId)
							.build();
			dmCrmManualPointChangePort.changePoint(request);
		} catch (Exception e) {
			log.debug("trade_finish handlePoint: {}", e.toString());
		}
	}

	private void handleSync(long companyId, long orderId) {
		try {
			if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
				return;
			}
			Optional<Map<String, Object>> headerOpt =
					orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
			if (headerOpt.isEmpty()) {
				return;
			}
			Map<String, Object> orderInfo = new LinkedHashMap<>(headerOpt.get());
			if (shouldSkipPointAndSyncGuards(orderInfo)) {
				return;
			}
			List<Map<String, Object>> orderItems = new ArrayList<>();
			for (Map<String, Object> row : orderNormalOrderItemsReadPort.listItems(companyId, orderId)) {
				orderItems.add(new LinkedHashMap<>(row));
			}
			List<Long> itemIds =
					orderItems.stream()
							.map(r -> toPrimitiveLong(r.get("item_id")))
							.filter(id -> id > 0L)
							.distinct()
							.collect(Collectors.toList());
			List<Map<String, Object>> goodsOverlay =
					goodsItemsListsByItemIdsReadPort.listRowsByItemIds(companyId, itemIds);
			Map<Long, Map<String, Object>> goodsByItemId = new LinkedHashMap<>();
			for (Map<String, Object> g : goodsOverlay) {
				long iid = toPrimitiveLong(g.get("item_id"));
				goodsByItemId.put(iid, g);
			}
			String orderClass = stringOrEmpty(orderInfo.get("order_class"));
			for (Map<String, Object> item : orderItems) {
				long itemId = toPrimitiveLong(item.get("item_id"));
				Map<String, Object> gs = goodsByItemId.get(itemId);
				item.putIfAbsent("is_gift", 0);
				int giftInt = 0;
				if (gs != null) {
					if (gs.containsKey("item_name")) {
						item.put("item_name", Objects.toString(gs.get("item_name"), ""));
					}
					if (gs.containsKey("goods_bn")) {
						item.put("goods_bn", Objects.toString(gs.get("goods_bn"), ""));
					}
					if (gs.containsKey("item_bn")) {
						item.put("item_bn", Objects.toString(gs.get("item_bn"), ""));
					}
					Object gGift = gs.get("is_gift");
					if (gGift instanceof Boolean b) {
						giftInt = b ? 1 : 0;
					} else if (gGift instanceof Number n) {
						giftInt = n.intValue() != 0 ? 1 : 0;
					} else {
						giftInt = parseInt(gGift) != 0 ? 1 : 0;
					}
					item.put("is_gift", giftInt);
				}
				BigDecimal numBd = bd(item.get("num"));
				BigDecimal totalLine = bdMoney(item.get("total_fee"));
				if (numBd.compareTo(BigDecimal.ZERO) > 0) {
					item.put(
							"item_fee_t",
							totalLine.divide(numBd, 5, RoundingMode.HALF_UP).toPlainString());
				} else {
					item.put("item_fee_t", "0");
				}
				if ("pointsmall".equals(orderClass)) {
					item.put("is_gift", 1);
				}
			}
			orderInfo.put("items", orderItems);
			int totalFeeFen = fenMoney(orderInfo.get("total_fee"));
			int freightFen = intOrZero(orderInfo.get("freight_fee"));
			orderInfo.put("total_fee", totalFeeFen - freightFen);
			long salesmanId = toPrimitiveLong(orderInfo.get("salesman_id"));
			if (salesmanId > 0L) {
				shoppingGuideSalespersonRowReadPort
						.loadShoppingGuideRow(companyId, salesmanId)
						.ifPresent(
								sg -> {
									orderInfo.put("clerkCode", Objects.toString(sg.get("work_userid"), ""));
									orderInfo.put("clerkName", Objects.toString(sg.get("name"), ""));
								});
			}
			long distributorId = toPrimitiveLong(orderInfo.get("sale_salesman_distributor_id"));
			if (distributorId > 0L) {
				dmCrmTradeFinishGuideDistributorReadPort
						.loadGuideDistributorRow(companyId, distributorId)
						.ifPresent(
								dr -> {
									orderInfo.put("storeCode", Objects.toString(dr.get("shop_code"), ""));
									orderInfo.put("storeName", Objects.toString(dr.get("name"), ""));
								});
			}
			orderInfo.putIfAbsent("usedMemberPoints", intOrZero(orderInfo.get("point_fee")));
			dmCrmTradeFinishOrderSyncPort.syncOrderForTradeFinish(companyId, String.valueOf(orderId), orderInfo);
		} catch (Exception e) {
			log.debug("trade_finish handleSync: {}", e.toString());
		}
	}

	private static boolean shouldSkipPointAndSyncGuards(Map<String, Object> orderInfo) {
		boolean emptyPre =
				!StringUtils.hasText(String.valueOf(orderInfo.getOrDefault("dm_point_preid", "")).trim());
		int pointFee = intOrZero(orderInfo.get("point_fee"));
		int pointUse = intOrZero(orderInfo.get("point_use"));
		return emptyPre && (pointFee > 0 || pointUse > 0);
	}

	private static long requireCompanyId(Map<String, Object> payload) {
		Object v = payload.get("company_id");
		long n = toPrimitiveLong(v);
		if (n <= 0L) {
			throw new IllegalArgumentException("company_id required");
		}
		return n;
	}

	private static long requireOrderId(Map<String, Object> payload) {
		Object v = payload.get("order_id");
		long n = toPrimitiveLong(v);
		if (n <= 0L) {
			throw new IllegalArgumentException("order_id required");
		}
		return n;
	}

	private static long toPrimitiveLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseInt(Object v) {
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int intOrZero(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return parseInt(v);
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static int fenMoney(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return parseInt(raw);
	}

	private static BigDecimal bd(Object v) {
		if (v == null) {
			return BigDecimal.ZERO;
		}
		if (v instanceof BigDecimal b) {
			return b;
		}
		if (v instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(v).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static BigDecimal bdMoney(Object raw) {
		return bd(raw);
	}
}
