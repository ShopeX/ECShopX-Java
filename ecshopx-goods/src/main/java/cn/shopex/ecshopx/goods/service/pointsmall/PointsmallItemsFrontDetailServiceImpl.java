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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.orders.domain.dto.ShippingTemplateRow;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsFrontDetailService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsFrontDetailServiceImpl implements PointsmallItemsFrontDetailService {

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemsH5DetailAssembler assembler;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ObjectMapper objectMapper;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final PointsmallFrontTdkGivenRenderService pointsmallFrontTdkGivenRenderService;
	private final StringRedisTemplate companysRedisTemplate;
	private final LangueProperties langueProperties;

	public PointsmallItemsFrontDetailServiceImpl(PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemsH5DetailAssembler assembler,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ObjectMapper objectMapper,
			TdkGivenSaveService tdkGivenSaveService,
			PointsmallFrontTdkGivenRenderService pointsmallFrontTdkGivenRenderService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			LangueProperties langueProperties) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.assembler = assembler;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.objectMapper = objectMapper;
		this.tdkGivenSaveService = tdkGivenSaveService;
		this.pointsmallFrontTdkGivenRenderService = pointsmallFrontTdkGivenRenderService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> getDetailPayload(HttpServletRequest request, long companyId, String pathItemId, String goodsIdQuery,
			String woaAppid) {
		long resolvedItemId;
		Object validatorSubject;

		if (StringUtils.hasText(goodsIdQuery)) {
			Long goodsKey = parsePositiveLongKey(goodsIdQuery.trim());
			if (goodsKey == null) {
				throw new ResourceException("商品不存在或者已下架");
			}
			PointsmallItems hit = selectApprovedDefaultByGoodsId(companyId, goodsKey);
			if (hit == null || hit.getItemId() == null) {
				throw new ResourceException("商品不存在或者已下架");
			}
			resolvedItemId = hit.getItemId();
			validatorSubject = resolvedItemId;
		} else {
			String path = pathItemId == null ? "" : pathItemId.trim();
			if (!StringUtils.hasText(path)) {
				throw new ResourceException("商品不存在或者已下架");
			}
			long pathPk;
			try {
				pathPk = Long.parseLong(path);
			} catch (NumberFormatException e) {
				throw new ResourceException("商品不存在或者已下架");
			}
			PointsmallItems hit = selectApprovedByItemId(companyId, pathPk);
			if (hit == null) {
				throw new ResourceException("商品不存在或者已下架");
			}
			resolvedItemId = hit.getItemId() != null ? hit.getItemId() : pathPk;
			validatorSubject = path;
		}

		if (!passesItemIdValidator(validatorSubject)) {
			return Map.of("item_id", 0);
		}

		long effectiveItemId = resolvedItemId;

		Map<String, Object> built = assembler.buildDetail(effectiveItemId, companyId, woaAppid);
		if (built == null || built.isEmpty()) {
			return Map.of("item_id", 0);
		}

		final LinkedHashMap<String, Object> result = new LinkedHashMap<>(built);
		result.put("activity_type", "normal");

		result.put("cur", buildCurMap(companyId));

		long templatesId = toLong(result.get("templates_id"));
		result.put("no_post", List.of());
		if (templatesId > 0) {
			Optional<ShippingTemplateRow> tpl = shippingTemplatesQueryRepository.selectTemplateById(companyId, templatesId);
			tpl.map(ShippingTemplateRow::getNopostConf).filter(StringUtils::hasText).ifPresent(raw -> {
				try {
					Object parsed = objectMapper.readValue(raw, Object.class);
					if (parsed instanceof List<?> || parsed instanceof Map<?, ?>) {
						result.put("no_post", parsed);
					}
				} catch (Exception ignored) {
				}
			});
		}

		Object storePick = result.get("item_total_store");
		if (storePick == null) {
			storePick = result.get("store");
		}
		result.put("store", storePick);

		long rateCompanyId = toLong(result.get("company_id"));
		result.put("rate_status", readRateStatusFromRedis(rateCompanyId));

		String isTdk = request.getParameter("is_tdk");
		if (isTdk != null && "1".equals(isTdk.trim())) {
			String countryCodeRaw = resolveCountryCodeForTdk(request);
			Map<String, Object> tdkSlice = tdkGivenSaveService.getGivenSetInfo("details", companyId, countryCodeRaw);
			Map<String, Object> tdkData = pointsmallFrontTdkGivenRenderService.render(tdkSlice, result);
			mergeTdkContentOverrides(result, tdkData);
			result.put("tdk_data", tdkData);
		}

		return result;
	}

	private void mergeTdkContentOverrides(Map<String, Object> result, Map<String, Object> tdkData) {
		Object rawTc = result.get("tdk_content");
		if (rawTc == null || !StringUtils.hasText(rawTc.toString())) {
			return;
		}
		try {
			Map<String, Object> content = objectMapper.readValue(rawTc.toString(), new TypeReference<Map<String, Object>>() {});
			if (content == null) {
				return;
			}
			Object t = content.get("title");
			if (t != null && StringUtils.hasText(t.toString())) {
				tdkData.put("title", t.toString());
			}
			Object md = content.get("mate_description");
			if (md != null && StringUtils.hasText(md.toString())) {
				tdkData.put("mate_description", md.toString());
			}
			Object mk = content.get("mate_keywords");
			if (mk != null && StringUtils.hasText(mk.toString())) {
				tdkData.put("mate_keywords", mk.toString());
			}
		} catch (Exception ignored) {
		}
	}

	private PointsmallItems selectApprovedDefaultByGoodsId(long companyId, long goodsId) {
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getGoodsId, goodsId).eq(PointsmallItems::getAuditStatus, "approved")
				.eq(PointsmallItems::getIsDefault, true).last("LIMIT 1");
		return pointsmallItemsMapper.selectOne(w);
	}

	private PointsmallItems selectApprovedByItemId(long companyId, long itemId) {
		if (itemId < 1L) {
			return null;
		}
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getItemId, itemId).eq(PointsmallItems::getAuditStatus, "approved")
				.last("LIMIT 1");
		return pointsmallItemsMapper.selectOne(w);
	}

	private static Long parsePositiveLongKey(String raw) {
		try {
			long v = Long.parseLong(raw);
			return v >= 1L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean passesItemIdValidator(Object subject) {
		if (subject instanceof Long l) {
			return l >= 1L;
		}
		String s = subject.toString().trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		if (!s.matches("^-?\\d+$")) {
			return false;
		}
		try {
			return Long.parseLong(s) >= 1L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private LinkedHashMap<String, Object> buildCurMap(long companyId) {
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		LinkedHashMap<String, Object> curMap = new LinkedHashMap<>();
		if (row.getId() != null) {
			curMap.put("id", String.valueOf(row.getId()));
		}
		curMap.put("company_id", String.valueOf(companyId));
		curMap.put("currency", row.getCurrency());
		curMap.put("title", row.getTitle());
		curMap.put("symbol", row.getSymbol());
		curMap.put("rate", row.getRate());
		curMap.put("is_default", Boolean.TRUE.equals(row.getIsDefault()));
		if (row.getUsePlatform() != null) {
			curMap.put("use_platform", row.getUsePlatform());
		}
		return curMap;
	}

	private boolean readRateStatusFromRedis(long companyId) {
		if (companyId <= 0L) {
			return false;
		}
		String key = "TradeRateSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return false;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("rate_status"), false);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean jsonNodeToBooleanStrict(JsonNode node, boolean defaultVal) {
		if (node == null || node.isNull()) {
			return defaultVal;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isInt()) {
			return node.asInt() != 0;
		}
		if (node.isTextual()) {
			String s = node.asText().trim();
			return "1".equals(s) || "true".equalsIgnoreCase(s);
		}
		return defaultVal;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private String resolveCountryCodeForTdk(HttpServletRequest request) {
		String q = request.getParameter("country_code");
		if (StringUtils.hasText(q)) {
			String t = q.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		String resolved = RequestLangTag.current(langueProperties);
		if (StringUtils.hasText(resolved) && !"zh-CN".equals(resolved)) {
			return resolved;
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw instanceof Map<?, ?> ud) {
			Object cc = ud.get("country_code");
			if (cc != null) {
				String s = cc.toString().trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return "zh-CN";
	}
}
