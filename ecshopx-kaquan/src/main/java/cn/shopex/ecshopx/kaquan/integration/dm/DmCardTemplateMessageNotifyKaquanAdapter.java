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

package cn.shopex.ecshopx.kaquan.integration.dm;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.kaquan.port.DmCardTemplateMessageNotifyKaquanPort;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDeleteFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardCreateService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardSetParamsService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountStandardCardUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DmCardTemplateMessageNotifyKaquanAdapter implements DmCardTemplateMessageNotifyKaquanPort {

	private static final Logger log = LoggerFactory.getLogger(DmCardTemplateMessageNotifyKaquanAdapter.class);

	private static final Set<String> ENVELOPE_KEYS = Set.of(
			"topic",
			"Topic",
			"msg_type",
			"MsgType",
			"content",
			"postdata",
			"sign",
			"msg_signature",
			"timestamp",
			"nonce",
			"event",
			"Event",
			"msg_event",
			"MsgEvent",
			"msg_Event",
			"msg_id",
			"authorizer_appid");

	private final DiscountStandardCardCreateService standardCardCreateService;
	private final DiscountStandardCardUpdateService standardCardUpdateService;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardDeleteFacadeService discountCardDeleteFacadeService;
	private final ObjectMapper objectMapper;

	public DmCardTemplateMessageNotifyKaquanAdapter(
			DiscountStandardCardCreateService standardCardCreateService,
			DiscountStandardCardUpdateService standardCardUpdateService,
			DiscountCardsMapper discountCardsMapper,
			DiscountCardDeleteFacadeService discountCardDeleteFacadeService,
			ObjectMapper objectMapper) {
		this.standardCardCreateService = standardCardCreateService;
		this.standardCardUpdateService = standardCardUpdateService;
		this.discountCardsMapper = discountCardsMapper;
		this.discountCardDeleteFacadeService = discountCardDeleteFacadeService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> handle(long companyId, Map<String, Object> mergedRequestInput, String authorizerAppid) {
		String topic = resolveTopicKey(mergedRequestInput);
		if ("sync_card_template_modify".equals(topic)) {
			return handleModify(companyId, mergedRequestInput, authorizerAppid);
		}
		if ("sync_card_template_delete".equals(topic)) {
			return handleDelete(companyId, mergedRequestInput, authorizerAppid);
		}
		if ("sync_card_template_create".equals(topic)) {
			Map<String, Object> postdata = buildPostdata(companyId, mergedRequestInput);
			return standardCardCreateService.createKaquan(postdata, authorizerAppid != null ? authorizerAppid : "");
		}
		log.warn("dm card-template notify: unsupported topic after normalization topic={}", topic);
		Map<String, Object> noop = new LinkedHashMap<>();
		noop.put("noop", Boolean.TRUE);
		noop.put("company_id", companyId);
		return noop;
	}

	private Map<String, Object> handleDelete(
			long companyId, Map<String, Object> mergedRequestInput, String authorizerAppid) {
		Map<String, Object> postdata = buildPostdata(companyId, mergedRequestInput);
		Long cardId = extractCardId(postdata);
		if (cardId == null) {
			log.warn(
					"dm card-template delete: missing card_id in payload after buildPostdata companyId={}",
					companyId);
			return modifyNoop(companyId);
		}
		String appid = authorizerAppid != null ? authorizerAppid : "";
		discountCardDeleteFacadeService.deleteDiscountCard(postdata, companyId, appid, false);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("card_id", cardId);
		out.put("company_id", companyId);
		return out;
	}

	private Map<String, Object> handleModify(
			long companyId, Map<String, Object> mergedRequestInput, String authorizerAppid) {
		Map<String, Object> postdata = buildPostdata(companyId, mergedRequestInput);
		Long cardId = extractCardId(postdata);
		if (cardId == null) {
			log.warn(
					"dm card-template modify: missing card_id in payload after buildPostdata companyId={}",
					companyId);
			return modifyNoop(companyId);
		}
		DiscountCards existing = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getCardId, cardId)
				.last("LIMIT 1"));
		if (existing == null) {
			log.warn(
					"dm card-template modify: no existing standard card companyId={} cardId={}",
					companyId,
					cardId);
			return modifyNoop(companyId);
		}
		return standardCardUpdateService.updateKaquan(
				postdata, authorizerAppid != null ? authorizerAppid : "", existing);
	}

	private static Map<String, Object> modifyNoop(long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("noop", Boolean.TRUE);
		m.put("company_id", companyId);
		return m;
	}

	private static String resolveTopicKey(Map<String, Object> merged) {
		if (merged == null) {
			return "";
		}
		Object t = merged.get("topic");
		if (t == null) {
			t = merged.get("Topic");
		}
		if (t == null) {
			return "";
		}
		return t.toString().trim().toLowerCase(Locale.ROOT);
	}

	private static Long extractCardId(Map<String, Object> postdata) {
		if (postdata == null) {
			return null;
		}
		Object v = postdata.get("card_id");
		if (v == null) {
			v = postdata.get("cardId");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> buildPostdata(long companyId, Map<String, Object> merged) {
		Map<String, Object> base = extractInnerCardPayload(merged);
		LinkedHashMap<String, Object> postdata = new LinkedHashMap<>(base);
		postdata.put("company_id", companyId);
		applyStandardCardDefaults(postdata);
		return postdata;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractInnerCardPayload(Map<String, Object> merged) {
		if (merged == null) {
			return new LinkedHashMap<>();
		}
		Object content = merged.get("content");
		if (content instanceof Map<?, ?> m) {
			return new LinkedHashMap<>((Map<String, Object>) m);
		}
		if (content instanceof String s && StringUtils.hasText(s)) {
			try {
				return new LinkedHashMap<>(objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {}));
			} catch (JsonProcessingException e) {
				log.warn("dm card-template notify: content is not valid JSON object", e);
				String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
				throw new BadRequestException("content is not valid JSON object: " + detail);
			}
		}
		Object postdata = merged.get("postdata");
		if (postdata instanceof Map<?, ?> m) {
			return new LinkedHashMap<>((Map<String, Object>) m);
		}
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : merged.entrySet()) {
			if (!ENVELOPE_KEYS.contains(e.getKey())) {
				copy.put(e.getKey(), e.getValue());
			}
		}
		return copy;
	}

	private static void applyStandardCardDefaults(Map<String, Object> postdata) {
		postdata.putIfAbsent("card_type", "normal");
		postdata.putIfAbsent("coupon_type", "cash");
		postdata.putIfAbsent("title", "dm-template");
		postdata.putIfAbsent("color", "Color010");
		postdata.putIfAbsent("description", "");
		postdata.putIfAbsent("date_type", "DATE_TYPE_FIX_TIME_RANGE");
		postdata.putIfAbsent("begin_date", 0);
		postdata.putIfAbsent("end_date", 0);
		postdata.putIfAbsent("use_bound", DiscountStandardCardSetParamsService.FOR_ALL_ITEMS);
		if (!postdata.containsKey("distributor_id")) {
			postdata.put("distributor_id", new ArrayList<String>());
		}
	}
}
