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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.wxa.WxaGenerateUrlLinkClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WxappWxaUrlLinkService {

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";

	private final WeappMapper weappMapper;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaGenerateUrlLinkClient wxaGenerateUrlLinkClient;

	public WxappWxaUrlLinkService(
			WeappMapper weappMapper,
			WechatAuthQueryService wechatAuthQueryService,
			WxaGenerateUrlLinkClient wxaGenerateUrlLinkClient) {
		this.weappMapper = weappMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaGenerateUrlLinkClient = wxaGenerateUrlLinkClient;
	}

	public Map<String, Object> wxaUrlLink(long companyId, Map<String, Object> mergedParams) {
		Map<String, Object> merged = mergedParams == null ? Map.of() : mergedParams;

		validatePath(merged);
		validateQuery(merged);
		validateEnvVersion(merged);

		LambdaQueryWrapper<Weapp> w = new LambdaQueryWrapper<>();
		w.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getTemplateName, TEMPLATE_NAME_YYKWEISHOP)
				.isNull(Weapp::getDeletedAt)
				.last("LIMIT 1");
		Weapp row = weappMapper.selectOne(w);
		if (row == null || !StringUtils.hasText(row.getAuthorizerAppid())) {
			throw new ResourceException("没有绑定小程序", 422);
		}

		String wxaAppid = row.getAuthorizerAppid().trim();
		wechatAuthQueryService
				.findBoundAuthByCompanyAndAuthorizerAppid(companyId, wxaAppid)
				.orElseThrow(() -> new ResourceException("没有绑定小程序", 422));

		String path = String.valueOf(merged.get("path")).trim();
		String queryStringForWechat = buildQueryStringForWechat(merged);

		Object evRaw = merged.get("env_version");
		String ev = evRaw == null ? "" : String.valueOf(evRaw).trim();
		String envVersionForWechat = StringUtils.hasText(ev) ? ev : "release";

		return wxaGenerateUrlLinkClient.generate(wxaAppid, path, queryStringForWechat, envVersionForWechat);
	}

	private static void validatePath(Map<String, Object> merged) {
		Object v = merged.get("path");
		if (v == null) {
			throw new ResourceException("小程序页面路径必填", 422);
		}
		if (v instanceof CharSequence cs) {
			if (!StringUtils.hasText(cs.toString().trim())) {
				throw new ResourceException("小程序页面路径必填", 422);
			}
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("小程序页面路径必填", 422);
		}
	}

	private static void validateQuery(Map<String, Object> merged) {
		Object raw = merged.get("query");
		if (raw == null) {
			throw new ResourceException("进入小程序时的query必填", 422);
		}
		if (raw instanceof CharSequence cs) {
			if (!StringUtils.hasText(cs.toString().trim())) {
				throw new ResourceException("进入小程序时的query必填", 422);
			}
			return;
		}
		if (raw instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				throw new ResourceException("进入小程序时的query必填", 422);
			}
			return;
		}
		if (raw instanceof Collection<?> coll && !(raw instanceof Map<?, ?>)) {
			if (coll.isEmpty()) {
				throw new ResourceException("进入小程序时的query必填", 422);
			}
			return;
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new ResourceException("进入小程序时的query必填", 422);
			}
			return;
		}
		if (raw instanceof Number || raw instanceof Boolean) {
			return;
		}
	}

	private static void validateEnvVersion(Map<String, Object> merged) {
		Object v = merged.get("env_version");
		if (v == null) {
			throw new ResourceException("请填写正确的小程序版本", 422);
		}
		String ev = v instanceof CharSequence ? ((CharSequence) v).toString().trim() : String.valueOf(v).trim();
		if (!StringUtils.hasText(ev)) {
			throw new ResourceException("请填写正确的小程序版本", 422);
		}
		if (!isAllowedEnvVersion(ev)) {
			throw new ResourceException("请填写正确的小程序版本", 422);
		}
	}

	private static boolean isAllowedEnvVersion(String ev) {
		return "release".equals(ev) || "trial".equals(ev) || "develop".equals(ev);
	}

	private static String buildQueryStringForWechat(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("query")) {
			return "";
		}
		Object rawQuery = mergedParams.get("query");
		if (rawQuery == null) {
			return "";
		}
		if (rawQuery instanceof Map<?, ?> m) {
			TreeMap<String, String> sorted = new TreeMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				sorted.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
			}
			UriComponentsBuilder qb = UriComponentsBuilder.newInstance();
			for (Map.Entry<String, String> e : sorted.entrySet()) {
				qb.queryParam(e.getKey(), e.getValue());
			}
			String q = qb.build().encode(StandardCharsets.UTF_8).getQuery();
			return q == null ? "" : q;
		}
		if (rawQuery instanceof Collection<?> coll && !(rawQuery instanceof Map<?, ?>)) {
			List<?> list = rawQuery instanceof List<?> l ? l : new ArrayList<>(coll);
			UriComponentsBuilder qb = UriComponentsBuilder.newInstance();
			for (int i = 0; i < list.size(); i++) {
				Object v = list.get(i);
				qb.queryParam(String.valueOf(i), v == null ? "" : String.valueOf(v));
			}
			String q = qb.build().encode(StandardCharsets.UTF_8).getQuery();
			return q == null ? "" : q;
		}
		if (rawQuery instanceof CharSequence) {
			return "";
		}
		if (rawQuery instanceof Number || rawQuery instanceof Boolean) {
			return "";
		}
		return "";
	}
}
