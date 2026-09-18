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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.wechat.WorkWechatCustomerContactRelationshipPort;
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
public class WorkWechatCustomerNotifyService {

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatCustomerContactRelationshipPort relationshipPort;
	private final WorkWechatCustomerEventXmlParser xmlParser;

	public WorkWechatCustomerNotifyService(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatCustomerContactRelationshipPort relationshipPort,
			WorkWechatCustomerEventXmlParser xmlParser) {
		this.workWechatConfigService = workWechatConfigService;
		this.relationshipPort = relationshipPort;
		this.xmlParser = xmlParser;
	}

	public ResponseEntity<?> handle(
			String corpid,
			boolean echostrParamPresent,
			Map<String, String> queryParams,
			String rawBody) {
		try {
			log.info("customer notify start corpid={} query={}", corpid, queryParams);
			if (rawBody != null && log.isDebugEnabled()) {
				log.debug("customer notify raw body length={}", rawBody.length());
			}
			Map<String, Object> config = workWechatConfigService.loadParsedWorkWechatConfigByCorpid(corpid);
			Object agentsObj = config.get("agents");
			if (!(agentsObj instanceof Map<?, ?> agents)) {
				throw new ResourceException("企业微信配置无效");
			}
			Object customerObj = agents.get("customer");
			if (!(customerObj instanceof Map<?, ?> customer)) {
				throw new ResourceException("企业微信配置无效");
			}
			String token = trimToEmpty(customer.get("token"));
			String aesKey = trimToEmpty(customer.get("aes_key"));
			String configCorpid = trimToEmpty(config.get("corpid"));
			Long companyIdOrNull = parseCompanyIdBoxed(config.get("company_id"));
			WXBizMsgCrypt wxcpt = new WXBizMsgCrypt(token, aesKey, configCorpid);
			String msgSig = queryParams == null ? "" : nullToEmpty(queryParams.get("msg_signature"));
			String ts = queryParams == null ? "" : nullToEmpty(queryParams.get("timestamp"));
			String nonce = queryParams == null ? "" : nullToEmpty(queryParams.get("nonce"));
			String echostr = queryParams == null ? "" : queryParams.get("echostr");
			if (echostrParamPresent) {
				String[] outEcho = new String[1];
				int err = wxcpt.verifyUrl(msgSig, ts, nonce, echostr, outEcho);
				if (err == WXBizMsgCrypt.OK) {
					log.info("customer notify verify url success");
					String plain = outEcho[0] == null ? "" : outEcho[0];
					return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(plain);
				}
				log.info("customer notify verify url fail errCode={}", err);
				return ResponseEntity.ok().build();
			}
			if (rawBody != null && !rawBody.isEmpty()) {
				String[] outXml = new String[1];
				int err = wxcpt.decryptMsg(msgSig, ts, nonce, rawBody, outXml);
				if (err == WXBizMsgCrypt.OK) {
					String xml = outXml[0];
					log.info("customer notify decrypted xml={}", xml);
					Map<String, Object> eventMap = xmlParser.toMap(xml);
					log.info("customer notify event map={}", eventMap);
					relationshipPort.handleCustomerContactEvent(companyIdOrNull, eventMap);
					return ResponseEntity.ok().build();
				}
				log.info("customer notify decrypt fail errCode={}", err);
				return ResponseEntity.ok().build();
			}
			return ResponseEntity.ok().build();
		} catch (ResourceException
				| BadRequestException
				| UnauthorizedException
				| ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			log.error("customer notify error", e);
			throw new ResourceException("error.");
		} finally {
			log.info("customer notify end corpid={}", corpid);
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

	private static Long parseCompanyIdBoxed(Object companyIdObj) {
		if (companyIdObj == null) {
			return null;
		}
		if (companyIdObj instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(companyIdObj).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
