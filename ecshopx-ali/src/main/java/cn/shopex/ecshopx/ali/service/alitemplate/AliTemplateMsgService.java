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

package cn.shopex.ecshopx.ali.service.alitemplate;

import cn.shopex.ecshopx.ali.service.h5.AlipayMiniEasySdkFactory;
import cn.shopex.ecshopx.ali.service.minisetting.AliMiniAppSettingInfoService;
import com.alipay.easysdk.factory.Factory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliTemplateMsgService {

	private static final Logger log = LoggerFactory.getLogger(AliTemplateMsgService.class);

	private static final Object SDK_LOCK = new Object();

	private static final String DEFAULT_LISTING_PAGE = "pages/goods/detail";

	private static final Map<String, List<String>> SOURCE_SCENES;

	static {
		Map<String, List<String>> m = new LinkedHashMap<>();
		m.put("logistics_order", List.of("paymentSucc", "payOrdersRemind", "orderDeliverySucc"));
		m.put("ziti_order", List.of("paymentSucc", "payOrdersRemind"));
		m.put("after_refund", List.of("aftersalesRefuse"));
		m.put("activity", List.of("registrationResultNotice"));
		m.put("member", List.of("memberCreateSucc"));
		m.put("coupon", List.of("userGetCardSucc"));
		m.put("goods", List.of("goodsArrivalNotice"));
		SOURCE_SCENES = Collections.unmodifiableMap(m);
	}

	private final AliOpenTemplateLibraryRedisAccessor templateLibraryRedisAccessor;
	private final AliMiniAppSettingInfoService aliMiniAppSettingInfoService;
	private final AlipayMiniEasySdkFactory alipayMiniEasySdkFactory;
	private final ObjectMapper objectMapper;

	public AliTemplateMsgService(
			AliOpenTemplateLibraryRedisAccessor templateLibraryRedisAccessor,
			AliMiniAppSettingInfoService aliMiniAppSettingInfoService,
			AlipayMiniEasySdkFactory alipayMiniEasySdkFactory,
			ObjectMapper objectMapper) {
		this.templateLibraryRedisAccessor = templateLibraryRedisAccessor;
		this.aliMiniAppSettingInfoService = aliMiniAppSettingInfoService;
		this.alipayMiniEasySdkFactory = alipayMiniEasySdkFactory;
		this.objectMapper = objectMapper;
	}

	public List<String> getValidTemplateIds(int companyId, String sourceType) {
		if (!StringUtils.hasText(sourceType)) {
			return List.of();
		}
		List<String> scenes = SOURCE_SCENES.get(sourceType.trim());
		if (scenes == null) {
			return List.of();
		}
		List<String> lists = new ArrayList<>();
		for (String scenesName : scenes) {
			templateLibraryRedisAccessor.getTemplate(companyId, scenesName).ifPresent(map -> {
				Object tid = map.get("template_id");
				lists.add(tid == null ? "" : String.valueOf(tid));
			});
		}
		return lists;
	}

	/**
	 * Sends an Alipay mini-program template message when configuration and template metadata allow it. Exceptions are
	 * swallowed by the job handler after logging.
	 */
	public void send(Map<String, Object> data, boolean forceFire) {
		try {
			if (!validateSendPayload(data)) {
				return;
			}
			long companyRaw = toLong(data.get("company_id"));
			int companyId = (int) Math.min(Math.max(companyRaw, 0L), Integer.MAX_VALUE);
			String scenesName = String.valueOf(data.get("scenes_name")).trim();
			String toUserId = String.valueOf(data.get("to_user_id")).trim();
			if (!forceFire
					&& templateLibraryRedisAccessor
							.resolveDispatchDelayMinutesFromTemplate(companyId, scenesName)
							.isPresent()) {
				return;
			}
			Optional<Map<String, Object>> tplOpt = templateLibraryRedisAccessor.getTemplate(companyId, scenesName);
			if (tplOpt.isEmpty()) {
				return;
			}
			String templateId = templateIdFromMap(tplOpt.get());
			if (!StringUtils.hasText(templateId)) {
				return;
			}
			Map<String, Object> setting = aliMiniAppSettingInfoService.getInfoByCompanyId(companyId);
			var config = alipayMiniEasySdkFactory.buildConfig(setting);
			String page = DEFAULT_LISTING_PAGE;
			Object pageQueryStr = data.get("page_query_str");
			if (pageQueryStr != null && StringUtils.hasText(String.valueOf(pageQueryStr))) {
				page = DEFAULT_LISTING_PAGE + "?" + String.valueOf(pageQueryStr).trim();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> innerData = (Map<String, Object>) data.get("data");
			String dataJson = buildTemplateDataJson(innerData);
			synchronized (SDK_LOCK) {
				Factory.setOptions(config);
				Factory.Marketing.TemplateMessage().send(toUserId, "", templateId, page, dataJson);
			}
		} catch (Exception e) {
			log.debug("AliTemplateMsg send skipped: {}", e.toString());
		}
	}

	private boolean validateSendPayload(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return false;
		}
		if (data.get("company_id") == null) {
			return false;
		}
		if (!StringUtils.hasText(String.valueOf(data.get("scenes_name")))) {
			return false;
		}
		if (!(data.get("data") instanceof Map<?, ?>)) {
			return false;
		}
		return StringUtils.hasText(String.valueOf(data.get("to_user_id")));
	}

	private static String templateIdFromMap(Map<String, Object> tpl) {
		Object tid = tpl.get("template_id");
		return tid == null ? "" : String.valueOf(tid).trim();
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}

	private String buildTemplateDataJson(Map<String, Object> inner) throws JsonProcessingException {
		ObjectNode root = objectMapper.createObjectNode();
		if (inner != null) {
			for (Map.Entry<String, Object> e : inner.entrySet()) {
				ObjectNode slot = objectMapper.createObjectNode();
				slot.put("value", e.getValue() == null ? "" : String.valueOf(e.getValue()).trim());
				root.set(e.getKey(), slot);
			}
		}
		return objectMapper.writeValueAsString(root);
	}
}
