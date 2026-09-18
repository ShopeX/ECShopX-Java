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

package cn.shopex.ecshopx.wechat.wxjava;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;

@Component
public class WxMaDataCubePosts {

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxMaDataCubePosts(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public JsonNode postDateRange(String wxaAppId, String url, String beginYmd, String endYmd) {
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("begin_date", beginYmd);
			body.put("end_date", endYmd);
			String json = objectMapper.writeValueAsString(body);
			String resp = wxJavaMaRuntime.ma(wxaAppId).post(url, json);
			return objectMapper.readTree(resp);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toBadRequestOrUnauthorized(e);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		}
	}
}
