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

package cn.shopex.ecshopx.payment.service.orderquery;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 支付渠道订单查询结果中 {@code data} 子树的 JSON 序列化（不转义非 ASCII）。
 */
final class AdaBsPayOrderQueryJsonMapper {

	private static final ObjectMapper INSTANCE = create();

	private AdaBsPayOrderQueryJsonMapper() {}

	static ObjectMapper get() {
		return INSTANCE;
	}

	private static ObjectMapper create() {
		ObjectMapper m = new ObjectMapper();
		m.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), false);
		return m;
	}
}
