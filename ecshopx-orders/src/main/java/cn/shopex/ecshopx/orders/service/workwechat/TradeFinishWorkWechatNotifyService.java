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

package cn.shopex.ecshopx.orders.service.workwechat;

import cn.shopex.ecshopx.common.dispatch.SendDeliveryWaitDeliveryNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendDeliveryWaitZiTiNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeFinishWorkWechatNotifyService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final SendDeliveryWaitDeliveryNoticeJobDispatchPublisher sendDeliveryWaitDeliveryNoticeJobDispatchPublisher;
	private final SendDeliveryWaitZiTiNoticeJobDispatchPublisher sendDeliveryWaitZiTiNoticeJobDispatchPublisher;

	public TradeFinishWorkWechatNotifyService(
			NormalOrdersMapper normalOrdersMapper,
			SendDeliveryWaitDeliveryNoticeJobDispatchPublisher sendDeliveryWaitDeliveryNoticeJobDispatchPublisher,
			SendDeliveryWaitZiTiNoticeJobDispatchPublisher sendDeliveryWaitZiTiNoticeJobDispatchPublisher) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.sendDeliveryWaitDeliveryNoticeJobDispatchPublisher = sendDeliveryWaitDeliveryNoticeJobDispatchPublisher;
		this.sendDeliveryWaitZiTiNoticeJobDispatchPublisher = sendDeliveryWaitZiTiNoticeJobDispatchPublisher;
	}

	public void dispatchTradeFinishWorkWechatDeliveryWaitJobs(Map<String, Object> tradeRowPayload) {
		if (tradeRowPayload == null) {
			return;
		}
		String companyIdStr = stringify(tradeRowPayload.get("company_id"));
		String orderIdStr = stringify(tradeRowPayload.get("order_id"));
		if (!StringUtils.hasText(companyIdStr) || !StringUtils.hasText(orderIdStr)) {
			return;
		}

		Long companyId = parseLongFlexible(companyIdStr);
		Long orderId = parseLongFlexible(orderIdStr);
		if (companyId == null || orderId == null) {
			return;
		}

		NormalOrders normalOrder =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (normalOrder == null) {
			return;
		}

		String receiptType = normalOrder.getReceiptType();
		if (!StringUtils.hasText(receiptType)) {
			return;
		}
		if ("ziti".equals(receiptType)) {
			sendDeliveryWaitZiTiNoticeJobDispatchPublisher.publish(companyIdStr.trim(), orderIdStr.trim());
			return;
		}
		if (!"logistics".equals(receiptType)) {
			return;
		}

		sendDeliveryWaitDeliveryNoticeJobDispatchPublisher.publish(companyIdStr.trim(), orderIdStr.trim());
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Long parseLongFlexible(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
