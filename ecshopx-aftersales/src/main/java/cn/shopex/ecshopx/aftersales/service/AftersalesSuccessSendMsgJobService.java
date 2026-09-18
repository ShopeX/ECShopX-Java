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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesSuccessWxaSubscribeSendPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AftersalesSuccessSendMsgJobService {

	private static final Logger log = LoggerFactory.getLogger(AftersalesSuccessSendMsgJobService.class);

	private static final String AFTERSALES_SUCCESS_REMARKS = "您的售后已审核成功，请填写回寄物流！";

	private final OrderTradeMiniProgramRecipientLookupPort orderTradeMiniProgramRecipientLookupPort;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesSuccessWxaSubscribeSendPort aftersalesSuccessWxaSubscribeSendPort;

	public void execute(long companyId, long orderId, long aftersalesBn) {
		try {
			Optional<MiniProgramTradeRecipient> recipient =
					orderTradeMiniProgramRecipientLookupPort.findRecipient(companyId, orderId);
			if (recipient.isEmpty()) {
				return;
			}
			MiniProgramTradeRecipient r = recipient.get();
			Aftersales aftersales =
					aftersalesMapper.selectOne(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.eq(Aftersales::getAftersalesBn, aftersalesBn)
									.last("LIMIT 1"));
			if (aftersales == null) {
				return;
			}
			Long linkedOrderId = aftersales.getOrderId();
			int refundCents = aftersales.getRefundFee() != null ? aftersales.getRefundFee() : 0;
			String refundYuan = (refundCents / 100) + "元";

			Map<String, Object> data = new LinkedHashMap<>();
			data.put(
					"order_id",
					linkedOrderId != null ? String.valueOf(linkedOrderId) : String.valueOf(orderId));
			data.put("refund_fee", refundYuan);
			data.put("remarks", AFTERSALES_SUCCESS_REMARKS);

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("company_id", companyId);
			payload.put("scenes_name", "aftersalesSuccess");
			payload.put("appid", r.wxaAppid());
			payload.put("openid", r.openId());
			payload.put("data", data);

			aftersalesSuccessWxaSubscribeSendPort.send(payload, false);
		} catch (RuntimeException e) {
			log.debug("aftersales success subscribe notify skipped: {}", e.toString());
		}
	}
}
