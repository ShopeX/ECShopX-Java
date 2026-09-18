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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.promotions.domain.DistributorPromotions;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.RegisterPromotionMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterDistributorCreateService {

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final DistributorPromotionsMapper distributorPromotionsMapper;
	private final RegisterPromotionMultiLangWriteService registerPromotionMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public RegisterDistributorCreateService(
			RegisterPromotionsMapper registerPromotionsMapper,
			DistributorPromotionsMapper distributorPromotionsMapper,
			RegisterPromotionMultiLangWriteService registerPromotionMultiLangWriteService,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.distributorPromotionsMapper = distributorPromotionsMapper;
		this.registerPromotionMultiLangWriteService = registerPromotionMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	/** String inputs: only empty after trim and exact {@code "0"} are false; any other non-empty string is true. */
	private static boolean isOpenTrueLoose(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (raw instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty()) {
				return false;
			}
			return !"0".equals(t);
		}
		if (raw instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (raw instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		return isOpenTrueLoose(String.valueOf(raw));
	}

	private static String normalizeIsOpenForStorage(Object raw) {
		if (raw == null) {
			return "false";
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue() ? "true" : "false";
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (d == (long) d) {
				return Long.toString((long) d);
			}
			return raw.toString().trim();
		}
		if (raw instanceof CharSequence s) {
			String t = s.toString().trim();
			return t.isEmpty() ? "false" : t;
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				return "false";
			}
			String t = raw.toString().trim();
			return t.isEmpty() ? "false" : t;
		}
		if (raw instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				return "false";
			}
			String t = raw.toString().trim();
			return t.isEmpty() ? "false" : t;
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() ? "false" : t;
	}

	private static boolean isEmptyAdTitleField(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof CharSequence s) {
			String t = s.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		return false;
	}

	private String serializePromotionsValue(Object raw, Locale locale) {
		if (raw == null) {
			return null;
		}
		try {
			if (raw instanceof String str) {
				String t = str.trim();
				if (t.startsWith("{") || t.startsWith("[")) {
					JsonNode node = objectMapper.readTree(t);
					return objectMapper.writeValueAsString(node);
				}
				return objectMapper.writeValueAsString(str);
			}
			if (raw instanceof JsonNode node) {
				return objectMapper.writeValueAsString(node);
			}
			JsonNode tree = objectMapper.valueToTree(raw);
			return objectMapper.writeValueAsString(tree);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.register.promotions_value_invalid", "促销方案格式错误", locale));
		}
	}

	private String serializeRegisterJumpPath(Object raw, Locale locale) {
		if (raw == null) {
			return "[]";
		}
		try {
			JsonNode tree;
			if (raw instanceof String s) {
				String t = s.trim();
				if (t.isEmpty()) {
					tree = objectMapper.createArrayNode();
				} else {
					tree = objectMapper.readTree(t);
				}
			} else {
				tree = objectMapper.valueToTree(raw);
			}
			if (!tree.isObject() && !tree.isArray()) {
				throw new BadRequestException(
						msg("promotions.register.register_jump_path_invalid", "跳转路径格式错误", locale));
			}
			return objectMapper.writeValueAsString(tree);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.register.register_jump_path_invalid", "跳转路径格式错误", locale));
		}
	}

	private Long parseDistributorListElement(Object o, Locale locale) {
		if (o == null) {
			throw new BadRequestException(
					msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
		}
	}

	private List<Long> normalizeDistributorIds(Object raw, Locale locale) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>(c.size());
			for (Object o : c) {
				out.add(parseDistributorListElement(o, locale));
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<Long> out = new ArrayList<>(arr.length);
			for (Object o : arr) {
				out.add(parseDistributorListElement(o, locale));
			}
			return out;
		}
		if (raw instanceof int[] arr) {
			List<Long> out = new ArrayList<>(arr.length);
			for (int v : arr) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof long[] arr) {
			List<Long> out = new ArrayList<>(arr.length);
			for (long v : arr) {
				out.add(v);
			}
			return out;
		}
		if (raw instanceof short[] arr) {
			List<Long> out = new ArrayList<>(arr.length);
			for (short v : arr) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof byte[] arr) {
			List<Long> out = new ArrayList<>(arr.length);
			for (byte v : arr) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof JsonNode node) {
			if (!node.isArray()) {
				throw new BadRequestException(
						msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
			}
			List<Long> out = new ArrayList<>();
			for (JsonNode el : node) {
				if (el == null || el.isNull()) {
					throw new BadRequestException(
							msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
				}
				if (el.isIntegralNumber()) {
					out.add(el.longValue());
				} else {
					out.add(parseDistributorListElement(el.asText(), locale));
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.startsWith("[")) {
				try {
					return objectMapper.readValue(t, new TypeReference<List<Long>>() {});
				} catch (JsonProcessingException e) {
					throw new BadRequestException(
							msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
				}
			}
			try {
				return List.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
			}
		}
		throw new BadRequestException(
				msg("promotions.register.distributor_id_invalid", "分销商 ID 列表格式不正确", locale));
	}

	private void applyRegisterColumnsFromPayload(
			RegisterPromotions entity,
			long companyId,
			Map<String, Object> data,
			Locale locale,
			String isOpenStoredLiteral) {
		entity.setCompanyId(companyId);
		entity.setIsOpen(isOpenStoredLiteral);
		if (data.containsKey("register_type") && ValuePresence.hasEffectiveValue(data.get("register_type"))) {
			entity.setRegisterType(String.valueOf(data.get("register_type")).trim());
		}
		if (data.containsKey("ad_title")) {
			entity.setAdTitle(Objects.toString(data.get("ad_title"), ""));
		}
		if (data.containsKey("ad_pic") && ValuePresence.hasEffectiveValue(data.get("ad_pic"))) {
			entity.setAdPic(String.valueOf(data.get("ad_pic")));
		}
		if (data.containsKey("promotions_value")) {
			entity.setPromotionsValue(serializePromotionsValue(data.get("promotions_value"), locale));
		}
		entity.setRegisterJumpPath(serializeRegisterJumpPath(data.get("register_jump_path"), locale));
	}

	private void persistRegisterPromotion(long companyId, Map<String, Object> data, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);

		if (isEmptyAdTitleField(data.get("ad_title"))) {
			throw new BadRequestException(msg("promotions.register.register_title_required", "请填写广告标题", locale));
		}
		if (isOpenTrueLoose(data.get("is_open")) && isEmptyAdTitleField(data.get("ad_pic"))) {
			throw new ResourceException(msg("promotions.register.please_select_ad_image", "请选择广告图", locale));
		}

		String effectiveRegisterType =
				(data.containsKey("register_type") && ValuePresence.hasEffectiveValue(data.get("register_type")))
						? String.valueOf(data.get("register_type")).trim()
						: "general";

		String isOpenStored = normalizeIsOpenForStorage(data.get("is_open"));
		RegisterPromotions entity;
		boolean updating = data.get("id") != null && ValuePresence.hasEffectiveValue(data.get("id"));
		if (updating) {
			long id;
			try {
				id = Long.parseLong(String.valueOf(data.get("id")).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(msg("promotions.register.id_invalid", "参数 id 格式不正确", locale));
			}
			RegisterPromotions existing =
					registerPromotionsMapper.selectOne(
							new LambdaQueryWrapper<RegisterPromotions>()
									.eq(RegisterPromotions::getId, id)
									.eq(RegisterPromotions::getCompanyId, companyId));
			if (existing == null) {
				throw new ResourceException(msg("promotions.register.no_update_data_found", "未查询到更新数据", locale));
			}
			entity = existing;
			applyRegisterColumnsFromPayload(entity, companyId, data, locale, isOpenStored);
			registerPromotionsMapper.updateById(entity);
		} else {
			entity = new RegisterPromotions();
			applyRegisterColumnsFromPayload(entity, companyId, data, locale, isOpenStored);
			registerPromotionsMapper.insert(entity);
		}

		registerPromotionMultiLangWriteService.upsertAdFields(entity.getId(), companyId, data, requestLangTag);

		if ("distributor".equals(effectiveRegisterType)) {
			List<Long> distIds = normalizeDistributorIds(data.get("distributor_id"), locale);
			if (distIds.isEmpty()) {
				throw new BadRequestException(
						msg("promotions.register.please_select_distributor", "请选择分销商", locale));
			}
			distributorPromotionsMapper.delete(
					new LambdaQueryWrapper<DistributorPromotions>()
							.eq(DistributorPromotions::getCompanyId, companyId)
							.eq(DistributorPromotions::getPromotionId, entity.getId())
							.eq(DistributorPromotions::getPromotionType, "register"));
			for (Long did : distIds) {
				DistributorPromotions row = new DistributorPromotions();
				row.setCompanyId(companyId);
				row.setDistributorId(did);
				row.setPromotionId(entity.getId());
				row.setPromotionType("register");
				distributorPromotionsMapper.insert(row);
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void createRegister(long companyId, Map<String, Object> data, String requestLangTag) {
		persistRegisterPromotion(companyId, data, requestLangTag);
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveRegisterPromotionsConfig(long companyId, Map<String, Object> data, String requestLangTag) {
		persistRegisterPromotion(companyId, data, requestLangTag);
	}
}
