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
import cn.shopex.ecshopx.orders.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderPartialCancelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Wxapp order cancellation: validates the association row, then routes pending-shipment whole-order
 * cancels through {@link AdminNormalOrderFullCancelService} (which schedules dispatch after commit).
 */
@Service
public class WxappOrderCancelService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;
	private final AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	public WxappOrderCancelService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			CommunityOrderRelActivityMapper communityOrderRelActivityMapper,
			AdminNormalOrderFullCancelService adminNormalOrderFullCancelService,
			AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.communityOrderRelActivityMapper = communityOrderRelActivityMapper;
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
		this.adminNormalOrderPartialCancelService = adminNormalOrderPartialCancelService;
	}

	public Map<String, Object> cancelOrder(HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		LinkedHashMap<String, Object> params = WxappOrderParamMergeSupport.applyDefaultsAndAuth(merged, auth);
		Object orderIdRaw = merged.get("order_id");
		if (orderIdRaw != null) {
			String trimmed = String.valueOf(orderIdRaw).trim();
			if (StringUtils.hasText(trimmed)) {
				params.put("order_id", trimmed);
			}
		}

		String orderIdStr = params.get("order_id") == null ? "" : String.valueOf(params.get("order_id")).trim();
		if (!StringUtils.hasText(orderIdStr)) {
			throw new BadRequestException("订单号必填");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号格式错误");
		}

		long companyId = longVal(params.get("company_id"));
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("订单号为" + orderIdNum + "的订单不存在");
		}

		boolean sameBuyer = userIdsLooselyEqual(assoc.getUserId(), auth.get("user_id"));
		if (sameBuyer) {
			params.put("mobile", stringVal(auth.get("mobile")));
			params.put("user_id", longVal(auth.get("user_id")));
			params.put("cancel_from", "buyer");
		} else if (chiefIdTruthy(auth.get("chief_id"))) {
			Long chiefId = parsePositiveLongStrict(auth.get("chief_id"));
			if (chiefId == null) {
				throw new ResourceException("只能取消自己开团的订单");
			}
			NormalOrders normalOrder =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderIdNum)
									.last("LIMIT 1"));
			if (normalOrder == null) {
				throw new ResourceException("订单不存在");
			}
			CommunityOrderRelActivity rel = communityOrderRelActivityMapper.selectById(orderIdNum);
			if (rel == null) {
				throw new ResourceException("只能取消自己开团的订单");
			}
			if (rel.getChiefId() == null) {
				throw new ResourceException("只能取消自己开团的订单");
			}
			if (!Objects.equals(rel.getChiefId(), chiefId)) {
				throw new ResourceException("只能取消自己开团的订单");
			}
			params.put("user_id", assoc.getUserId() != null ? assoc.getUserId() : 0L);
			params.put("mobile", assoc.getMobile() == null ? "" : assoc.getMobile().trim());
			params.put("chief_id", chiefId);
			params.put("cancel_from", "chief");
		} else {
			throw new ResourceException("订单数据异常");
		}

		if (!"normal".equalsIgnoreCase(nullToEmpty(assoc.getOrderType()))) {
			throw new ResourceException("实体类订单才能取消订单！");
		}

		if ("WAIT_GROUPS_SUCCESS".equalsIgnoreCase(assoc.getOrderStatus())
				&& "groups".equalsIgnoreCase(assoc.getOrderClass())) {
			throw new ResourceException("拼团订单完成之前不允许取消订单！");
		}

		String cancelReason = trimToEmpty(params.get("cancel_reason"));
		String otherReason = trimToEmpty(params.get("other_reason"));
		String reasonText = firstNonBlank(cancelReason, otherReason);

		long userIdForRow = longVal(params.get("user_id"));
		String mobile = stringVal(params.get("mobile"));
		String cancelFrom = stringVal(params.get("cancel_from"));

		String delivery = assoc.getDeliveryStatus() == null ? "" : assoc.getDeliveryStatus().trim();
		if ("PENDING".equalsIgnoreCase(delivery)) {
			return adminNormalOrderFullCancelService.execute(
					companyId,
					"buyer",
					0L,
					0L,
					userIdForRow,
					mobile,
					orderIdNum,
					reasonText,
					params,
					cancelFrom);
		}
		return adminNormalOrderPartialCancelService.execute(
				companyId, "buyer", 0L, 0L, userIdForRow, orderIdNum, reasonText);
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		return "";
	}

	private static String trimToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static boolean userIdsLooselyEqual(Object orderUserId, Object authUserId) {
		return normalizeUserIdString(orderUserId).equals(normalizeUserIdString(authUserId));
	}

	private static String normalizeUserIdString(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return "";
		}
		try {
			return String.valueOf(Long.parseLong(s));
		} catch (NumberFormatException e) {
			return s;
		}
	}

	private static boolean chiefIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static Long parsePositiveLongStrict(Object raw) {
		if (raw == null) {
			return null;
		}
		try {
			long v;
			if (raw instanceof Number n) {
				v = n.longValue();
			} else {
				v = Long.parseLong(String.valueOf(raw).trim());
			}
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long longVal(Object v) {
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
}
