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

package cn.shopex.ecshopx.adapay.service.callback.handler;

import cn.shopex.ecshopx.adapay.domain.AdapayPaymentReverse;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymentReverseMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayCallbackPaymentReverseHandler {

	private final AdapayPaymentReverseMapper adapayPaymentReverseMapper;
	private final ObjectMapper objectMapper;

	public AdapayCallbackPaymentReverseHandler(
			AdapayPaymentReverseMapper adapayPaymentReverseMapper, ObjectMapper objectMapper) {
		this.adapayPaymentReverseMapper = adapayPaymentReverseMapper;
		this.objectMapper = objectMapper;
	}

	public List<Object> succeeded(Map<String, Object> data) {
		Map<String, Object> original = new LinkedHashMap<>(data);
		String orderNo = text(data.get("order_no"));
		String status = text(data.get("status"));
		String responseJson;
		try {
			responseJson = objectMapper.writeValueAsString(original);
		} catch (JsonProcessingException e) {
			responseJson = "{}";
		}
		if (StringUtils.hasText(orderNo)) {
			adapayPaymentReverseMapper.update(
					null,
					new LambdaUpdateWrapper<AdapayPaymentReverse>()
							.eq(AdapayPaymentReverse::getOrderNo, orderNo)
							.set(AdapayPaymentReverse::getStatus, status)
							.set(AdapayPaymentReverse::getResponseParams, responseJson));
		}
		return List.of("success");
	}

	public List<Object> failed(Map<String, Object> data) {
		return List.of("success");
	}

	private static String text(Object o) {
		return o == null ? "" : o.toString();
	}
}
