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

package cn.shopex.ecshopx.payment.service.dto;

import org.springframework.util.StringUtils;

/**
 * Subset of {@link com.alipay.api.response.AlipayTradePayResponse} for orders-layer mapping without a
 * compile dependency on {@code com.alipay.api} in ecshopx-orders.
 */
public record AlipayTradePayShallowResult(
		String code, String msg, String subCode, String subMsg, String tradeNo) {

	public static AlipayTradePayShallowResult from(com.alipay.api.response.AlipayTradePayResponse r) {
		if (r == null) {
			return new AlipayTradePayShallowResult("", "", "", "", "");
		}
		return new AlipayTradePayShallowResult(
				trim(r.getCode()),
				trim(r.getMsg()),
				trim(r.getSubCode()),
				trim(r.getSubMsg()),
				trim(r.getTradeNo()));
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	public boolean hasTextTradeNo() {
		return StringUtils.hasText(tradeNo);
	}
}
