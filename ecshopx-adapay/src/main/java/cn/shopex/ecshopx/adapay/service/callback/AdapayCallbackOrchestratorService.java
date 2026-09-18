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

package cn.shopex.ecshopx.adapay.service.callback;

import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackCorpMemberHandler;
import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackCorpMemberUpdateHandler;
import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackPaymentHandler;
import cn.shopex.ecshopx.adapay.service.callback.handler.AdapayCallbackPaymentReverseHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayCallbackOrchestratorService {

	private static final Logger log = LoggerFactory.getLogger(AdapayCallbackOrchestratorService.class);

	private final AdapaySignatureVerifier adapaySignatureVerifier;
	private final AdapayCallbackEventTypeRegistry adapayCallbackEventTypeRegistry;
	private final AdapayCallbackDingoArrayResponseBodyFactory adapayCallbackDingoArrayResponseBodyFactory;
	private final AdapayCallbackPaymentHandler adapayCallbackPaymentHandler;
	private final AdapayCallbackCorpMemberHandler adapayCallbackCorpMemberHandler;
	private final AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler;
	private final AdapayCallbackPaymentReverseHandler adapayCallbackPaymentReverseHandler;
	private final ObjectMapper objectMapper;

	public AdapayCallbackOrchestratorService(
			AdapaySignatureVerifier adapaySignatureVerifier,
			AdapayCallbackEventTypeRegistry adapayCallbackEventTypeRegistry,
			AdapayCallbackDingoArrayResponseBodyFactory adapayCallbackDingoArrayResponseBodyFactory,
			AdapayCallbackPaymentHandler adapayCallbackPaymentHandler,
			AdapayCallbackCorpMemberHandler adapayCallbackCorpMemberHandler,
			AdapayCallbackCorpMemberUpdateHandler adapayCallbackCorpMemberUpdateHandler,
			AdapayCallbackPaymentReverseHandler adapayCallbackPaymentReverseHandler,
			ObjectMapper objectMapper) {
		this.adapaySignatureVerifier = adapaySignatureVerifier;
		this.adapayCallbackEventTypeRegistry = adapayCallbackEventTypeRegistry;
		this.adapayCallbackDingoArrayResponseBodyFactory = adapayCallbackDingoArrayResponseBodyFactory;
		this.adapayCallbackPaymentHandler = adapayCallbackPaymentHandler;
		this.adapayCallbackCorpMemberHandler = adapayCallbackCorpMemberHandler;
		this.adapayCallbackCorpMemberUpdateHandler = adapayCallbackCorpMemberUpdateHandler;
		this.adapayCallbackPaymentReverseHandler = adapayCallbackPaymentReverseHandler;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> handle(Map<String, Object> bodyMap) {
		Map<String, Object> safe = bodyMap == null ? Collections.emptyMap() : bodyMap;
		log.info("AdaPay callback body: {}", safe);
		String type = text(safe.get("type"));
		String dataStr = safe.get("data") == null ? "" : safe.get("data").toString();
		String sign = safe.get("sign") == null ? "" : safe.get("sign").toString();
		adapaySignatureVerifier.verifyOrThrow(sign, dataStr);
		log.info("AdaPay callback: signature verified");
		if (!StringUtils.hasText(dataStr)) {
			throw new BadRequestException("回调 data 不是合法 JSON");
		}
		Map<String, Object> postData;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed = objectMapper.readValue(dataStr, Map.class);
			postData = parsed == null ? Map.of() : parsed;
		} catch (JsonProcessingException e) {
			throw new BadRequestException("回调 data 不是合法 JSON");
		}
		if ("payment.close.succeeded".equals(type) || "payment.close.failed".equals(type)) {
			return adapayCallbackDingoArrayResponseBodyFactory.buildSuccessEnvelope(Map.of());
		}
		AdapayCallbackEventTypeRegistry.HandlerKind kind = adapayCallbackEventTypeRegistry.resolve(type);
		if (kind == null) {
			throw new BadRequestException("unknown type");
		}
		Object inner =
				switch (kind) {
					case PAYMENT -> adapayCallbackPaymentHandler.succeeded(postData);
					case CORP_MEMBER -> adapayCallbackCorpMemberHandler.succeeded(postData);
					case CORP_MEMBER_UPDATE -> adapayCallbackCorpMemberUpdateHandler.succeeded(postData);
					case PAYMENT_REVERSE -> adapayCallbackPaymentReverseHandler.succeeded(postData);
				};
		return adapayCallbackDingoArrayResponseBodyFactory.buildSuccessEnvelope(inner);
	}

	private static String text(Object o) {
		return o == null ? "" : o.toString();
	}
}
