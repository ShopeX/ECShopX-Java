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

package cn.shopex.ecshopx.payment.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.payment.service.AlipayNotifyFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.PLAIN,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = false,
		notFound = false)
@RestController("paymentAdminV1PaymentNotify")
@RequestMapping("/api/v1")
public class PaymentNotifyController {

	private final AlipayNotifyFacade alipayNotifyFacade;

	public PaymentNotifyController(AlipayNotifyFacade alipayNotifyFacade) {
		this.alipayNotifyFacade = alipayNotifyFacade;
	}

	@PostMapping(value = "/alipay/notify", name = "支付宝异步通知")
	public ResponseEntity<String> handle(HttpServletRequest request) {
		return alipayNotifyFacade.handle(request);
	}
}
