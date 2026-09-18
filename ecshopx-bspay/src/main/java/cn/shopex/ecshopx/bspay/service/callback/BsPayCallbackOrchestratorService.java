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

package cn.shopex.ecshopx.bspay.service.callback;

import cn.shopex.ecshopx.bspay.config.BsPayCallbackProperties;
import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.service.WithdrawApplyService;
import cn.shopex.ecshopx.bspay.support.BsPayCallbackSignatureVerifier;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.service.bspay.BsPayOrdersTradePaymentCallbackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BsPayCallbackOrchestratorService {

	private static final Map<String, String> EVENT_WHITELIST = Map.of(
			"pay.wx_lite", "PAYMENT",
			"pay.wx_pub", "PAYMENT",
			"pay.wx_qr", "PAYMENT",
			"pay.alipay_wap", "PAYMENT",
			"pay.alipay_qr", "PAYMENT",
			"withdraw.bspay", "WITHDRAW");

	private final BsPayCallbackProperties properties;
	private final ObjectMapper objectMapper;
	private final BsPayOrdersTradePaymentCallbackService bsPayOrdersTradePaymentCallbackService;
	private final WithdrawApplyService withdrawApplyService;

	public BsPayCallbackOrchestratorService(
			BsPayCallbackProperties properties,
			ObjectMapper objectMapper,
			BsPayOrdersTradePaymentCallbackService bsPayOrdersTradePaymentCallbackService,
			WithdrawApplyService withdrawApplyService) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.bsPayOrdersTradePaymentCallbackService = bsPayOrdersTradePaymentCallbackService;
		this.withdrawApplyService = withdrawApplyService;
	}

	public Object handle(String eventType, Map<String, Object> requestBody) {
		log.info("bspay callback eventType:{} body:{}", eventType, requestBody);
		String publicKey = properties.getRsaPublicKey();
		if (!StringUtils.hasText(publicKey == null ? "" : publicKey.trim())) {
			throw new BadRequestException("支付回调公钥未配置");
		}
		String respDataStr = Objects.toString(requestBody.getOrDefault("resp_data", ""), "");
		String sign = Objects.toString(requestBody.getOrDefault("sign", ""), "");
		if (!BsPayCallbackSignatureVerifier.verifyRsaSha256(sign, respDataStr, publicKey.trim())) {
			log.error("回调：签名验证失败");
			throw new BadRequestException("签名验证失败");
		}
		log.info("回调：签名ok");
		if (!EVENT_WHITELIST.containsKey(eventType)) {
			throw new BadRequestException("unknown type");
		}
		Map<String, Object> postData;
		try {
			postData = objectMapper.readValue(respDataStr, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("回调数据无效");
		}
		if (postData == null) {
			throw new BadRequestException("回调数据无效");
		}
		return switch (Objects.requireNonNull(EVENT_WHITELIST.get(eventType))) {
			case "PAYMENT" -> {
				bsPayOrdersTradePaymentCallbackService.handlePaymentNotify(postData, eventType);
				yield List.of("success");
			}
			case "WITHDRAW" -> {
				String reqSeqId = Objects.toString(postData.getOrDefault("req_seq_id", ""), "");
				log.info("bspay withdraw callback req_seq_id:{}", reqSeqId);
				WithdrawApply row = withdrawApplyService.getByReqSeqId(reqSeqId);
				if (row == null) {
					log.info("未找到记录 req_seq_id:{}", reqSeqId);
				} else {
					withdrawApplyService.handleWithdrawNotify(postData, row);
				}
				yield Map.of("success", true);
			}
			default -> throw new BadRequestException("unknown type");
		};
	}
}
