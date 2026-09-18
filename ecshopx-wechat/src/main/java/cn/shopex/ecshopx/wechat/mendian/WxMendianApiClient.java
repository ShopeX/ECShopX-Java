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

package cn.shopex.ecshopx.wechat.mendian;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import cn.shopex.ecshopx.wechat.wxjava.WxMaPoiApiUrls;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxMendianApiClient {

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxMendianApiClient(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public static boolean errcodeIsZero(JsonNode n) {
		return n != null
				&& !n.isMissingNode()
				&& !n.isNull()
				&& ((n.isNumber() && n.intValue() == 0)
						|| (n.isTextual() && "0".equals(n.asText())));
	}

	public JsonNode getMerchantAuditInfo(String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("查询门店小程序失败.");
		}
		try {
			String raw =
					wxJavaMaRuntime.ma(authorizerAppid.trim()).get(WxMaPoiApiUrls.GET_MERCHANT_AUDIT_INFO, "");
			return parseJsonOrThrowQueryFailed(raw);
		} catch (WxErrorException e) {
			throw new ResourceException("查询门店小程序失败.");
		}
	}

	public JsonNode getStoreList(String authorizerAppid, int offset, int limit) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("offset", offset);
		body.put("limit", limit);
		try {
			String raw =
					wxJavaMaRuntime.ma(authorizerAppid.trim()).post(WxMaPoiApiUrls.GET_STORE_LIST, body);
			JsonNode root = parseJsonOrThrowQueryFailed(raw);
			if (!errcodeIsZero(root.path("errcode"))) {
				throw new ResourceException("查询门店小程序失败.");
			}
			return root;
		} catch (WxErrorException e) {
			throw new ResourceException("查询门店小程序失败.");
		}
	}

	public void delStore(String authorizerAppid, String poiId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("poi_id", poiId);
		try {
			String raw = wxJavaMaRuntime.ma(authorizerAppid.trim()).post(WxMaPoiApiUrls.DEL_STORE, body);
			JsonNode root = parseJsonOrThrowDelStoreFailed(raw);
			if (!errcodeIsZero(root.path("errcode"))) {
				throw new ResourceException("删除微信门店失败.");
			}
		} catch (WxErrorException e) {
			throw new ResourceException("删除微信门店失败.");
		}
	}

	private JsonNode parseJsonOrThrowDelStoreFailed(String raw) {
		if (raw == null || raw.isEmpty()) {
			throw new ResourceException("删除微信门店失败.");
		}
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new ResourceException("删除微信门店失败.");
		}
	}

	private JsonNode parseJsonOrThrowQueryFailed(String raw) {
		if (raw == null || raw.isEmpty()) {
			throw new ResourceException("查询门店小程序失败.");
		}
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new ResourceException("查询门店小程序失败.");
		}
	}
}
