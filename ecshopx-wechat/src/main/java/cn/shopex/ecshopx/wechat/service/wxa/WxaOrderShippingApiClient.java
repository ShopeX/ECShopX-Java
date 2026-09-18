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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

@Service
public class WxaOrderShippingApiClient {

	private static final String GET_ORDER_URL = "wxa/sec/order/get_order";
	private static final String UPLOAD_URL = "wxa/sec/order/upload_shipping_info";

	private static final TypeReference<LinkedHashMap<String, Object>> ROOT_MAP_TYPE = new TypeReference<>() {};

	private static final String INVALID_RESPONSE_ERRMSG = "invalid wechat response";

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxaOrderShippingApiClient(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getOrder(String wxaAppId, String transactionId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("transaction_id", transactionId);
		return postReturnMap(wxaAppId, GET_ORDER_URL, body);
	}

	public Map<String, Object> uploadShippingInfo(String wxaAppId, Map<String, Object> params) {
		return postReturnMap(wxaAppId, UPLOAD_URL, params);
	}

	private Map<String, Object> postReturnMap(String wxaAppId, String url, Map<String, Object> body) {
		WxMaService svc = wxJavaMaRuntime.ma(wxaAppId.trim());
		try {
			String raw = svc.post(url, body);
			return parseToMap(raw);
		} catch (WxErrorException e) {
			return wxErrorToMap(e);
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		}
	}

	private LinkedHashMap<String, Object> parseToMap(String raw) {
		if (raw == null || raw.isEmpty()) {
			return invalidResponseMap();
		}
		try {
			LinkedHashMap<String, Object> map = objectMapper.readValue(raw, ROOT_MAP_TYPE);
			return map != null ? map : invalidResponseMap();
		} catch (Exception e) {
			return invalidResponseMap();
		}
	}

	/** Non-retryable sentinel (must not be -1 / 10060012 / 10060019). */
	private static LinkedHashMap<String, Object> invalidResponseMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("errcode", -999);
		map.put("errmsg", INVALID_RESPONSE_ERRMSG);
		return map;
	}

	private static LinkedHashMap<String, Object> wxErrorToMap(WxErrorException e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		if (e.getError() != null) {
			map.put("errcode", e.getError().getErrorCode());
			map.put("errmsg", e.getError().getErrorMsg());
		} else {
			map.put("errcode", -1);
			map.put("errmsg", e.getMessage());
		}
		return map;
	}
}
