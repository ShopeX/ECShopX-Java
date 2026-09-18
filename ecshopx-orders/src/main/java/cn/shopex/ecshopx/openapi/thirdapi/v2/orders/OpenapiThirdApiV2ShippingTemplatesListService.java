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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.orders.service.shippingtemplate.OpenapiShippingTemplateListCriteria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ShippingTemplatesListService {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final CommonLangModReadService commonLangModReadService;

	public OpenapiThirdApiV2ShippingTemplatesListService(
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			CommonLangModReadService commonLangModReadService) {
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.commonLangModReadService = commonLangModReadService;
	}

	public Map<String, Object> list(long companyId, int page, int pageSize) {
		OpenapiShippingTemplateListCriteria criteria =
				new OpenapiShippingTemplateListCriteria(companyId, 0L);
		long total = shippingTemplatesQueryRepository.countByOpenApiCriteria(criteria);
		long offset = (long) (page - 1) * pageSize;
		List<ShippingTemplates> entities =
				shippingTemplatesQueryRepository.selectPageByOpenApiCriteria(criteria, offset, pageSize);

		List<Map<String, Object>> rows = entities.stream().map(this::toLangMergeRow).toList();
		commonLangModReadService.mergeShippingTemplateListNamesForLocale(companyId, rows, null);
		List<Map<String, Object>> formattedRows =
				rows.stream().map(this::formatOpenApiTemplateRow).toList();
		return formatListStruct(total, formattedRows, page, pageSize);
	}

	private Map<String, Object> toLangMergeRow(ShippingTemplates e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("template_id", e.getTemplateId());
		m.put("supplier_id", e.getSupplierId());
		m.put("name", e.getName());
		m.put("is_free", e.getIsFree());
		m.put("valuation", e.getValuation());
		m.put("protect", e.getProtect());
		m.put("protect_rate", formatDecimalPlain(e.getProtectRate(), 3));
		m.put("minprice", formatDecimalPlain(e.getMinprice(), 2));
		m.put("status", e.getStatus());
		m.put("fee_conf", e.getFeeConf());
		m.put("nopost_conf", e.getNopostConf());
		m.put("free_conf", e.getFreeConf());
		m.put("update_time", e.getUpdateTime());
		return m;
	}

	private Map<String, Object> formatOpenApiTemplateRow(Map<String, Object> row) {
		Map<String, Object> formatted = new LinkedHashMap<>(row);
		Object updateTime = row.get("update_time");
		if (updateTime instanceof Integer epoch) {
			formatted.put("update_time", formatEpochSeconds(epoch));
		} else if (updateTime instanceof Number n) {
			formatted.put("update_time", formatEpochSeconds(n.intValue()));
		}
		return formatted;
	}

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatDecimalPlain(BigDecimal value, int scale) {
		if (value == null) {
			return null;
		}
		return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
	}
}
