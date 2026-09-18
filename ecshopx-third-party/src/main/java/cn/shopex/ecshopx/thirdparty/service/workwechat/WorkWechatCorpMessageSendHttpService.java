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

package cn.shopex.ecshopx.thirdparty.service.workwechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.constant.WxCpApiPathConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("workWechatCorpMessageSendHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.work-wechat.corp-app",
		name = "http-enabled",
		havingValue = "true")
@RequiredArgsConstructor
public class WorkWechatCorpMessageSendHttpService implements WorkWechatCorpMessageSendPort {

	private static final Logger log = LoggerFactory.getLogger(WorkWechatCorpMessageSendHttpService.class);

	private final ObjectMapper objectMapper;

	@Override
	public void postMessageSend(String accessToken, String jsonBody) {
		if (!StringUtils.hasText(accessToken) || jsonBody == null) {
			return;
		}
		WxCpDefaultConfigImpl cfg = new WxCpDefaultConfigImpl();
		cfg.setCorpId("_");
		cfg.setCorpSecret("_");
		cfg.updateAccessToken(accessToken, 7200);
		WxCpServiceImpl wxCp = new WxCpServiceImpl();
		wxCp.setWxCpConfigStorage(cfg);
		try {
			String resp = wxCp.post(WxCpApiPathConsts.Message.MESSAGE_SEND, jsonBody);
			if (!StringUtils.hasText(resp)) {
				return;
			}
			JsonNode node = objectMapper.readTree(resp);
			int errcode = node.path("errcode").asInt(0);
			if (errcode != 0) {
				log.debug("work wechat message/send err body={}", resp);
			}
		} catch (WxErrorException e) {
			log.debug("work wechat message/send wx error", e);
		} catch (Exception e) {
			log.debug("work wechat message/send request failed", e);
		}
	}
}
