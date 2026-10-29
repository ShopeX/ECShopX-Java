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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.service.WxaCardUsedTemplateSendPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Real send for coupon receive / registration result subscribe scenes. Skips {@code cardUsedSucc}
 * (not in WxaTemplateSceneDefinitions).
 */
@Component
@Primary
@ConditionalOnProperty(
		name = "ecshopx.wechat.wxa-card-template-send.enabled",
		havingValue = "true",
		matchIfMissing = true)
public class WxaCardUsedTemplateSendPortImpl implements WxaCardUsedTemplateSendPort {

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";

	private static final Set<String> ALLOWED_SCENES =
			Set.of("userGetCardSucc", "registrationResultNotice");

	private final WeappMapper weappMapper;
	private final WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;
	private final WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService;

	public WxaCardUsedTemplateSendPortImpl(
			WeappMapper weappMapper,
			WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort,
			WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService) {
		this.weappMapper = weappMapper;
		this.wxaMemberOpenIdLookupPort = wxaMemberOpenIdLookupPort;
		this.wxaTemplateMsgActivityRemindSendService = wxaTemplateMsgActivityRemindSendService;
	}

	@Override
	public void send(long companyId, long userId, String templateScene, Map<String, String> keywordData) {
		String scene = templateScene == null ? "" : templateScene.trim();
		if (!ALLOWED_SCENES.contains(scene)) {
			return;
		}
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		String wxaAppid = resolveWxaAppId(companyId);
		if (!StringUtils.hasText(wxaAppid)) {
			return;
		}
		String openid = wxaMemberOpenIdLookupPort.resolveOpenId(userId, wxaAppid);
		if (!StringUtils.hasText(openid)) {
			return;
		}
		Map<String, Object> data = new LinkedHashMap<>();
		if (keywordData != null) {
			for (Map.Entry<String, String> e : keywordData.entrySet()) {
				if (e.getKey() == null) {
					continue;
				}
				data.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
			}
		}
		Map<String, Object> payload = new LinkedHashMap<>(8);
		payload.put("scenes_name", scene);
		payload.put("company_id", companyId);
		payload.put("appid", wxaAppid);
		payload.put("openid", openid);
		payload.put("data", data);
		wxaTemplateMsgActivityRemindSendService.send(payload, true);
	}

	private String resolveWxaAppId(long companyId) {
		Weapp row =
				weappMapper.selectOne(
						new LambdaQueryWrapper<Weapp>()
								.eq(Weapp::getCompanyId, companyId)
								.eq(Weapp::getTemplateName, TEMPLATE_NAME_YYKWEISHOP)
								.last("LIMIT 1"));
		if (row == null || !StringUtils.hasText(row.getAuthorizerAppid())) {
			return "";
		}
		return row.getAuthorizerAppid().trim();
	}
}
