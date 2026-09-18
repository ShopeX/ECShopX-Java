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

package cn.shopex.ecshopx.promotions.integration.orders;

import cn.shopex.ecshopx.common.port.orders.TradeFinishPaymentSuccWxaSubscribeSendPort;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TradeFinishPaymentSuccWxaSubscribeSendAdapter implements TradeFinishPaymentSuccWxaSubscribeSendPort {

	private final WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService;

	public TradeFinishPaymentSuccWxaSubscribeSendAdapter(
			WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService) {
		this.wxaTemplateMsgActivityRemindSendService = wxaTemplateMsgActivityRemindSendService;
	}

	@Override
	public void send(Map<String, Object> wxopenTemplatePayload, boolean forceFire) {
		wxaTemplateMsgActivityRemindSendService.send(wxopenTemplatePayload, forceFire);
	}
}
