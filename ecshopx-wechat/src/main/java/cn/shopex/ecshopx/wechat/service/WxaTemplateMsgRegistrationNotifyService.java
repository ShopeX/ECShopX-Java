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

import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaTemplateMsgRegistrationNotifyService {

	private static final Logger log = LoggerFactory.getLogger(WxaTemplateMsgRegistrationNotifyService.class);

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";

	private final WeappMapper weappMapper;
	private final WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;
	private final WxaCardUsedTemplateSendPort templateSendPort;

	public WxaTemplateMsgRegistrationNotifyService(
			WeappMapper weappMapper,
			WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort,
			WxaCardUsedTemplateSendPort templateSendPort) {
		this.weappMapper = weappMapper;
		this.wxaMemberOpenIdLookupPort = wxaMemberOpenIdLookupPort;
		this.templateSendPort = templateSendPort;
	}

	public void sendRegistrationResult(long companyId, long userId, Map<String, String> keywordData) {
		try {
			String wxaAppid = resolveWxaAppId(companyId);
			if (!StringUtils.hasText(wxaAppid)) {
				return;
			}
			String openid = wxaMemberOpenIdLookupPort.resolveOpenId(userId, wxaAppid);
			if (!StringUtils.hasText(openid)) {
				return;
			}
			templateSendPort.send(companyId, userId, "registrationResultNotice", keywordData);
		} catch (RuntimeException e) {
			log.debug("registration result template notify skipped: {}", e.getMessage());
		}
	}

	private String resolveWxaAppId(long companyId) {
		Weapp row = weappMapper.selectOne(new LambdaQueryWrapper<Weapp>()
				.eq(Weapp::getCompanyId, companyId)
				.eq(Weapp::getTemplateName, TEMPLATE_NAME_YYKWEISHOP)
				.last("LIMIT 1"));
		return row == null ? "" : stringVal(row.getAuthorizerAppid());
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
