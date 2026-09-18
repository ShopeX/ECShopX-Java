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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;

@Service
public class WechatOfficialAccountDataCubeUserStatsClient {

	private final ObjectMapper objectMapper;
	private final WxJavaMpRuntime wxJavaMpRuntime;

	public WechatOfficialAccountDataCubeUserStatsClient(
			ObjectMapper objectMapper, WxJavaMpRuntime wxJavaMpRuntime) {
		this.objectMapper = objectMapper;
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public JsonNode postUserSummary(String accessToken, String beginYmd, String endYmd) {
		return postJson(WxMpApiUrl.DataCube.GET_USER_SUMMARY, accessToken, dateRangeBody(beginYmd, endYmd));
	}

	public JsonNode postUserCumulate(String accessToken, String beginYmd, String endYmd) {
		return postJson(WxMpApiUrl.DataCube.GET_USER_CUMULATE, accessToken, dateRangeBody(beginYmd, endYmd));
	}

	private static Map<String, Object> dateRangeBody(String beginYmd, String endYmd) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("begin_date", beginYmd);
		body.put("end_date", endYmd);
		return body;
	}

	private JsonNode postJson(WxMpApiUrl endpoint, String accessToken, Map<String, Object> body) {
		try {
			String raw = wxJavaMpRuntime.mpBearer(accessToken).post(endpoint, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			int ec = root.path("errcode").asInt(0);
			if (ec == 0) {
				return root;
			}
			String msg = root.path("errmsg").asText("微信接口错误");
			if (ec == 40001 || ec == 40014 || ec == 41001 || ec == 42001) {
				throw new UnauthorizedException(msg);
			}
			if (ec == -1) {
				throw new BadRequestException("微信系统繁忙, 请稍后重试", 500);
			}
			throw new BadRequestException(msg, ec);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toBadRequestOrUnauthorized(e);
		} catch (UnauthorizedException | BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		}
	}
}
