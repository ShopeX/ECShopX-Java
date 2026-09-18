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

package cn.shopex.ecshopx.orders.service.group;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.SubOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.common.dispatch.OmeTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 拼团成功后将团员订单置为已支付，并在服务单场景下发放次卡/权益、实体单场景下发布 OME/ERP 同步事件。
 */
@Service
@RequiredArgsConstructor
public class GroupPromotionOrderPayedService {

	private static final int SUB_ORDER_LIST_LIMIT = 100;

	private final ServiceOrdersMapper serviceOrdersMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SubOrdersMapper subOrdersMapper;
	private final RightsMapper rightsMapper;
	private final TradeMapper tradeMapper;
	private final ObjectMapper objectMapper;
	private final OmeTradeUpdateDispatchPublisher omeTradeUpdateDispatchPublisher;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	public void markServiceGroupOrderPayedAndTryGrantRights(
			long companyId, long memberId, long orderId) {
		serviceOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<ServiceOrders>()
						.eq(ServiceOrders::getCompanyId, companyId)
						.eq(ServiceOrders::getOrderId, orderId)
						.set(ServiceOrders::getOrderStatus, "PAYED"));
		orderAssociationsMapper.update(
				null,
				new LambdaUpdateWrapper<OrderAssociations>()
						.eq(OrderAssociations::getOrderId, orderId)
						.set(OrderAssociations::getOrderStatus, "PAYED"));
		if (memberId > 0L) {
			addNewRights(companyId, memberId, orderId);
		}
	}

	public void markNormalGroupOrderPayedAndPublishErpSync(
			long companyId, long memberId, long orderId) {
		normalOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.set(NormalOrders::getOrderStatus, "PAYED"));
		orderAssociationsMapper.update(
				null,
				new LambdaUpdateWrapper<OrderAssociations>()
						.eq(OrderAssociations::getOrderId, orderId)
						.set(OrderAssociations::getOrderStatus, "PAYED"));
		Trade trade = findPrimaryTrade(companyId, orderId);
		Map<String, Object> erp = new LinkedHashMap<>();
		erp.put("company_id", companyId);
		erp.put("order_id", String.valueOf(orderId));
		if (trade != null) {
			erp.put("trade_id", trade.getTradeId());
		}
		erp.put("user_id", memberId);
		erp.put("order_class", "normal_groups");
		thirdPartyTradeUpdateDispatchPublisher.publish(Map.copyOf(erp));
		Map<String, Object> omeSubset = new LinkedHashMap<>();
		omeSubset.put("company_id", companyId);
		omeSubset.put("order_id", String.valueOf(orderId));
		omeSubset.put("order_class", "normal_groups");
		omeSubset.put("user_id", memberId);
		omeTradeUpdateDispatchPublisher.publish(Map.copyOf(omeSubset));
	}

	private void addNewRights(long companyId, long userId, long orderId) {
		ServiceOrders order =
				serviceOrdersMapper.selectOne(
						new LambdaQueryWrapper<ServiceOrders>()
								.eq(ServiceOrders::getCompanyId, companyId)
								.eq(ServiceOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}
		String consumeType = order.getConsumeType();
		if (!"all".equals(consumeType) && !"every".equals(consumeType)) {
			return;
		}
		List<SubOrders> subList =
				subOrdersMapper.selectList(
						new LambdaQueryWrapper<SubOrders>()
								.eq(SubOrders::getOrderId, orderId)
								.eq(SubOrders::getCompanyId, companyId)
								.orderByDesc(SubOrders::getLabelId)
								.last("LIMIT " + SUB_ORDER_LIST_LIMIT));
		if (subList == null || subList.isEmpty()) {
			return;
		}
		if ("all".equals(consumeType)) {
			int start;
			int end;
			if ("DATE_TYPE_FIX_TIME_RANGE".equals(order.getDateType())
					&& order.getBeginDate() != null
					&& order.getEndDate() != null) {
				start = order.getBeginDate();
				end = order.getEndDate();
			} else if ("DATE_TYPE_FIX_TERM".equals(order.getDateType()) && order.getFixedTerm() != null) {
				start = startOfTodayEpoch();
				end = (int) (start + 86400L * order.getFixedTerm() - 1);
			} else {
				return;
			}
			List<Map<String, Object>> labelInfos = new ArrayList<>();
			for (SubOrders v : subList) {
				addLabel(labelInfos, v.getLabelId(), v.getLabelName());
			}
			String rightsFrom =
					order.getOrderSource() != null && "shop".equals(order.getOrderSource())
							? "代客下单获取"
							: "购买获取";
			insertOneRights(
					companyId,
					userId,
					order,
					order.getTitle(),
					"",
					parseItemNumLong(order.getItemNum()),
					0L,
					start,
					end,
					labelInfos,
					Boolean.FALSE,
					rightsFrom,
					mobileOrEmpty(order.getMobile()),
					2,
					null);
			return;
		}
		for (SubOrders v : subList) {
			int start = startOfTodayEpoch();
			long lt = v.getLimitTime() == null ? 0L : v.getLimitTime();
			int end = (int) (start + 86400L * lt + 86399L);
			List<Map<String, Object>> oneLabel = new ArrayList<>();
			addLabel(oneLabel, v.getLabelId(), v.getLabelName());
			insertOneRights(
					companyId,
					userId,
					order,
					v.getItemName(),
					safeString(v.getLabelName()),
					v.getNum() == null ? 0L : v.getNum(),
					0L,
					start,
					end,
					oneLabel,
					Boolean.TRUE,
					(order.getOrderSource() != null && "shop".equals(order.getOrderSource()))
							? "代客下单获取"
							: "购买获取",
					mobileOrEmpty(order.getMobile()),
					v.getIsNotLimitNum() == null ? 2 : v.getIsNotLimitNum(),
					safeString(order.getOperatorDesc()));
		}
	}

	private void insertOneRights(
			long companyId,
			long userId,
			ServiceOrders order,
			String rightsName,
			String rightsSubname,
			long totalNum,
			long totalConsum,
			int start,
			int end,
			List<Map<String, Object>> labelInfos,
			Boolean canReservation,
			String rightsFrom,
			String mobile,
			int isNotLimit,
			String operatorDesc) {
		Rights r = new Rights();
		r.setUserId(userId);
		r.setCompanyId(companyId);
		r.setCanReservation(canReservation);
		r.setRightsName(rightsName);
		r.setRightsSubname(rightsSubname);
		r.setTotalNum(totalNum);
		r.setTotalConsumNum(totalConsum);
		r.setStartTime(start);
		r.setEndTime(end);
		r.setOrderId(order.getOrderId());
		r.setRightsFrom(rightsFrom);
		r.setMobile(mobile);
		r.setIsNotLimitNum(isNotLimit);
		r.setOperatorDesc(operatorDesc);
		r.setStatus("valid");
		int now = (int) (System.currentTimeMillis() / 1000L);
		r.setCreated(now);
		r.setUpdated(now);
		try {
			r.setLabelInfos(objectMapper.writeValueAsString(labelInfos));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("label_infos 序列化失败", e);
		}
		rightsMapper.insert(r);
	}

	private static int startOfTodayEpoch() {
		long dayMillis =
				(System.currentTimeMillis() + java.util.TimeZone.getDefault().getRawOffset()) / 86400000L
						* 86400000L
						- java.util.TimeZone.getDefault().getRawOffset();
		return (int) (dayMillis / 1000L);
	}

	private static void addLabel(
			List<Map<String, Object>> out, Long labelId, String labelName) {
		if (labelId == null) {
			return;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("label_id", labelId);
		m.put("label_name", labelName);
		out.add(m);
	}

	private static long parseItemNumLong(String itemNum) {
		if (!StringUtils.hasText(itemNum)) {
			return 0L;
		}
		String t = itemNum.trim();
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ex) {
			try {
				return Long.parseLong(t.split("[^0-9]")[0]);
			} catch (Exception e) {
				return 0L;
			}
		}
	}

	private static String mobileOrEmpty(String m) {
		return m == null ? "" : m;
	}

	private static String safeString(String s) {
		return s == null ? "" : s;
	}

	private Trade findPrimaryTrade(long companyId, long orderId) {
		return tradeMapper.selectOne(
				new LambdaQueryWrapper<Trade>()
						.eq(Trade::getOrderId, String.valueOf(orderId))
						.eq(Trade::getCompanyId, String.valueOf(companyId))
						.eq(Trade::getTradeState, "SUCCESS")
						.last("LIMIT 1"));
	}
}
