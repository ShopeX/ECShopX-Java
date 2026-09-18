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

package cn.shopex.ecshopx.wechat.service.openplatform;

import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 调用微信开放平台「创建卡券」接口（与公众号卡券 create 一致）。
 */
@Service
public class WechatOpenPlatformCardCreateClient {

	private static final Logger log = LoggerFactory.getLogger(WechatOpenPlatformCardCreateClient.class);

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public WechatOpenPlatformCardCreateClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	/**
	 * @param cardType 微信侧类型名，如 DISCOUNT、CASH
	 * @param baseInfo 卡券 base_info
	 * @param advancedInfo 卡券 advanced_info
	 * @param especial 与卡类型对应的专有字段（如 discount、least_cost），将并入类型子对象
	 * @return 成功返回 card_id，失败返回 null
	 */
	public String createCard(String authorizerAppId, String cardType, Map<String, Object> baseInfo,
			Map<String, Object> advancedInfo, Map<String, Object> especial) {
		if (!StringUtils.hasText(authorizerAppId) || !StringUtils.hasText(cardType)) {
			return null;
		}
		String typeKey = innerKeyForCardType(cardType);
		if (typeKey == null) {
			log.warn("wechat card create: unsupported cardType {}", cardType);
			return null;
		}
		Map<String, Object> typeBody = new LinkedHashMap<>();
		typeBody.put("base_info", baseInfo);
		typeBody.put("advanced_info", advancedInfo);
		if (especial != null) {
			typeBody.putAll(especial);
		}
		Map<String, Object> cardPayload = new LinkedHashMap<>();
		cardPayload.put("card_type", cardType);
		cardPayload.put(typeKey, typeBody);
		Map<String, Object> root = Map.of("card", cardPayload);
		try {
			String raw =
					wxJavaMpRuntime.mp(authorizerAppId).post(WxMpApiUrl.Card.CARD_CREATE, root);
			JsonNode node = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			int errcode = node.path("errcode").asInt(-1);
			if (errcode == 0) {
				String cardId = node.path("card_id").asText(null);
				if (StringUtils.hasText(cardId)) {
					return cardId;
				}
				return null;
			}
			log.warn("wechat card create failed errcode={} errmsg={}", errcode, node.path("errmsg").asText(""));
			return null;
		} catch (WxErrorException e) {
			log.warn("wechat card create wx error: {}", e.getMessage());
			return null;
		} catch (Exception e) {
			log.warn("wechat card create request error: {}", e.getMessage());
			return null;
		}
	}

	private static String innerKeyForCardType(String cardType) {
		return switch (cardType) {
			case "DISCOUNT" -> "discount";
			case "CASH" -> "cash";
			case "GROUPON" -> "groupon";
			case "GENERAL_COUPON" -> "general_coupon";
			case "GIFT" -> "gift";
			default -> null;
		};
	}
}
