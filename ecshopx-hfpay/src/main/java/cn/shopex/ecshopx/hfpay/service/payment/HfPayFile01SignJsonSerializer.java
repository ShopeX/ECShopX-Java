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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

/**
 * file01 签名字节串：与业务字段插入顺序一致，且 JSON 序列化等价于默认 {@code json_encode}（非 ASCII 与斜杠转义）。
 */
@Component
public class HfPayFile01SignJsonSerializer {

	private final ObjectMapper signMapper;

	public HfPayFile01SignJsonSerializer() {
		this.signMapper = new ObjectMapper();
		this.signMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
		this.signMapper.getFactory().configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES.mappedFeature(), true);
	}

	public String toSignJsonString(String merCustId, String attachNo, String transType, String attachType) {
		LinkedHashMap<String, String> data = new LinkedHashMap<>();
		data.put("version", "10");
		data.put("mer_cust_id", merCustId);
		data.put("attach_no", attachNo);
		data.put("trans_type", transType);
		data.put("attach_type", attachType);
		try {
			return signMapper.writeValueAsString(data);
		} catch (Exception e) {
			throw new ResourceException("签名错误");
		}
	}
}
