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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.wechat.callback.WXBizMsgCrypt;
import cn.shopex.ecshopx.workwechat.support.WorkWechatCustomerEventXmlParser;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatAppNotifyService {

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatCustomerEventXmlParser xmlParser;

	public WorkWechatAppNotifyService(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatCustomerEventXmlParser xmlParser) {
		this.workWechatConfigService = workWechatConfigService;
		this.xmlParser = xmlParser;
	}

	public ResponseEntity<?> handle(
			String corpid,
			boolean echostrParamPresent,
			Map<String, String> queryParams,
			String rawBody) {
		log.info("--------------------- notify start ---------------------");
		log.info("{}", queryParams);
		if (rawBody != null && log.isDebugEnabled()) {
			log.debug("notify raw body length={}", rawBody.length());
		}

		Map<String, Object> config = workWechatConfigService.loadParsedWorkWechatConfigByCorpid(corpid);
		Object agentsObj = config.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			throw new ResourceException("企业微信配置无效");
		}
		Object appObj = agents.get("app");
		if (!(appObj instanceof Map<?, ?> app)) {
			throw new ResourceException("企业微信配置无效");
		}
		String token = trimToEmpty(app.get("token"));
		String aesKey = trimToEmpty(app.get("aes_key"));
		if (!StringUtils.hasText(token) || !StringUtils.hasText(aesKey)) {
			throw new ResourceException("企业微信配置无效");
		}
		String configCorpid = trimToEmpty(config.get("corpid"));
		WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(token, aesKey, configCorpid);

		String msgSig = queryParams == null ? "" : nullToEmpty(queryParams.get("msg_signature"));
		String ts = queryParams == null ? "" : nullToEmpty(queryParams.get("timestamp"));
		String nonce = queryParams == null ? "" : nullToEmpty(queryParams.get("nonce"));
		String echostr = queryParams == null ? "" : queryParams.get("echostr");

		try {
			if (echostrParamPresent) {
				String[] outEcho = new String[1];
				int err = wxcpt.verifyUrl(msgSig, ts, nonce, echostr, outEcho);
				if (err == WXBizMsgCrypt.OK) {
					log.info("--------------------- customer notify success ---------------------");
					String plain = outEcho[0] == null ? "" : outEcho[0];
					log.info("{}", plain);
					return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(plain);
				}
				log.info("--------------------- customer notify fail ---------------------");
				log.info("{}", err);
				return ResponseEntity.ok().build();
			}
			if (rawBody != null && !rawBody.isEmpty()) {
				String[] outXml = new String[1];
				int err = wxcpt.decryptMsg(msgSig, ts, nonce, rawBody, outXml);
				if (err == WXBizMsgCrypt.OK) {
					String xml = outXml[0];
					log.info("--------------------- customer notify success ---------------------");
					log.info("XML info:{}", xml);
					try {
						Map<String, Object> eventMap = xmlParser.toMap(xml);
						log.info("response:{}", eventMap);
					} catch (Exception e) {
						log.warn("notify xml parse failed", e);
					}
					return ResponseEntity.ok().build();
				}
				log.info("--------------------- customer notify fail ---------------------");
				log.info("{}", err);
				return ResponseEntity.ok().build();
			}
			return ResponseEntity.ok().build();
		} finally {
			log.info("--------------------- notify end ---------------------");
		}
	}

	private static String trimToEmpty(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
