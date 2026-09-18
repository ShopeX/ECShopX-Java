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

package cn.shopex.ecshopx.payment.api.open;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.payment.service.WxpayNotifyFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.PLAIN,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = false,
		notFound = false)
@RestController("paymentOpenWxpayNotify")
@RequestMapping("/wechatAuth/wxpay")
public class WxpayNotifyController {

	private final WxpayNotifyFacade wxpayNotifyFacade;

	public WxpayNotifyController(WxpayNotifyFacade wxpayNotifyFacade) {
		this.wxpayNotifyFacade = wxpayNotifyFacade;
	}

	@PostMapping(
			value = "/notify",
			name = "微信支付异步通知",
			consumes = {MediaType.APPLICATION_XML_VALUE, "text/xml", MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE},
			produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<String> handle(HttpServletRequest request) {
		return wxpayNotifyFacade.handle(request);
	}
}
