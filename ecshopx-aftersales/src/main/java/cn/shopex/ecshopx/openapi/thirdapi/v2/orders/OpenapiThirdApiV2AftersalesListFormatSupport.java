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

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiThirdApiV2AftersalesListFormatSupport {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2AftersalesListFormatSupport(
			AftersalesDetailMapper aftersalesDetailMapper,
			DistributorDefaultAftersalesAddressReadPort distributorDefaultAftersalesAddressReadPort,
			ObjectMapper objectMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.distributorDefaultAftersalesAddressReadPort = distributorDefaultAftersalesAddressReadPort;
		this.objectMapper = objectMapper;
	}

	public record EnrichedRow(
			Aftersales entity, Map<String, Object> resolvedAddress, List<AftersalesDetail> details) {}

	public List<EnrichedRow> enrichRows(List<Aftersales> entities, long companyId) {
		Set<Long> distributorIds = new LinkedHashSet<>();
		for (Aftersales entity : entities) {
			Long distributorId = entity.getDistributorId();
			if (distributorId != null && distributorId > 0L) {
				distributorIds.add(distributorId);
			}
		}

		Map<Long, Map<String, Object>> defaultAddrByDist = new LinkedHashMap<>();
		for (Long distributorId : distributorIds) {
			distributorDefaultAftersalesAddressReadPort
					.findDefaultAddress(companyId, distributorId)
					.ifPresent(map -> defaultAddrByDist.put(distributorId, map));
		}

		List<EnrichedRow> rows = new ArrayList<>(entities.size());
		for (Aftersales entity : entities) {
			Map<String, Object> resolvedAddress = null;
			long distributorId = entity.getDistributorId() != null ? entity.getDistributorId() : 0L;
			if (needDefaultAddress(entity.getAftersalesAddress())
					&& distributorId > 0L
					&& defaultAddrByDist.containsKey(distributorId)) {
				resolvedAddress = defaultAddrByDist.get(distributorId);
			}

			List<AftersalesDetail> detailRows =
					aftersalesDetailMapper.selectList(
							new LambdaQueryWrapper<AftersalesDetail>()
									.eq(AftersalesDetail::getCompanyId, companyId)
									.eq(AftersalesDetail::getAftersalesBn, entity.getAftersalesBn())
									.eq(AftersalesDetail::getUserId, entity.getUserId()));

			rows.add(new EnrichedRow(entity, resolvedAddress, detailRows));
		}
		return rows;
	}

	public Map<String, Object> formatOpenApiAftersalesRow(EnrichedRow row) {
		Aftersales a = row.entity();
		Map<String, Object> formatted = new LinkedHashMap<>();
		formatted.put("aftersales_bn", String.valueOf(a.getAftersalesBn()));
		formatted.put("order_id", String.valueOf(a.getOrderId()));
		formatted.put("aftersales_type", a.getAftersalesType());
		formatted.put("aftersales_status", a.getAftersalesStatus());
		formatted.put("progress", a.getProgress());
		formatted.put("refund_fee", a.getRefundFee());
		formatted.put("refund_point", a.getRefundPoint());
		formatted.put("reason", a.getReason());
		formatted.put("description", a.getDescription());
		formatted.put("evidence_pic", parseEvidencePic(a.getEvidencePic()));
		formatted.put("refuse_reason", a.getRefuseReason());
		formatted.put("memo", a.getMemo());
		formatted.put("sendback_data", parseJsonObject(a.getSendbackData()));
		formatted.put("sendconfirm_data", parseJsonObject(a.getSendconfirmData()));
		formatted.put("create_time", formatEpochSeconds(a.getCreateTime()));
		formatted.put("update_time", formatEpochSeconds(a.getUpdateTime()));
		formatted.put("aftersales_address", resolveAftersalesAddress(row));
		formatted.put("detail", formatDetail(row.details()));
		return formatted;
	}

	public List<Map<String, Object>> formatDetailRows(List<AftersalesDetail> details) {
		return formatDetail(details);
	}

	public List<AftersalesDetail> loadDetailRowsForOpenApiDetail(long companyId, long aftersalesBn) {
		return aftersalesDetailMapper.selectList(
				new LambdaQueryWrapper<AftersalesDetail>()
						.eq(AftersalesDetail::getCompanyId, companyId)
						.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
						.orderByDesc(AftersalesDetail::getCreateTime));
	}

	public Map<String, Object> formatOpenApiAftersalesDetailRow(
			Aftersales a, List<AftersalesDetail> details, String refundBn) {
		Map<String, Object> formatted = new LinkedHashMap<>();
		formatted.put("aftersales_bn", String.valueOf(a.getAftersalesBn()));
		formatted.put("order_id", String.valueOf(a.getOrderId()));
		formatted.put("aftersales_type", a.getAftersalesType());
		formatted.put("aftersales_status", a.getAftersalesStatus());
		formatted.put("progress", a.getProgress());
		formatted.put("refund_fee", a.getRefundFee());
		formatted.put("refund_point", a.getRefundPoint());
		formatted.put("reason", a.getReason());
		formatted.put("description", a.getDescription());
		formatted.put("evidence_pic", parseEvidencePic(a.getEvidencePic()));
		formatted.put("refuse_reason", a.getRefuseReason());
		formatted.put("memo", a.getMemo());
		formatted.put("sendback_data", parseJsonObject(a.getSendbackData()));
		formatted.put("create_time", formatEpochSeconds(a.getCreateTime()));
		formatted.put("update_time", formatEpochSeconds(a.getUpdateTime()));
		formatted.put("aftersales_address", resolveRawAftersalesAddress(a.getAftersalesAddress()));
		formatted.put("detail", formatDetail(details));
		formatted.put("refund_bn", refundBn != null ? refundBn : "");
		return formatted;
	}

	public Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private Object resolveAftersalesAddress(EnrichedRow row) {
		if (row.resolvedAddress() != null) {
			return row.resolvedAddress();
		}
		return resolveRawAftersalesAddress(row.entity().getAftersalesAddress());
	}

	private Object resolveRawAftersalesAddress(String raw) {
		Map<String, Object> parsed = parseJsonObject(raw);
		return parsed != null ? parsed : new LinkedHashMap<>();
	}

	private List<Map<String, Object>> formatDetail(List<AftersalesDetail> details) {
		if (details == null || details.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>(details.size());
		for (AftersalesDetail d : details) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("goods_id", d.getGoodsId());
			m.put("item_bn", d.getItemBn());
			m.put("item_name", d.getItemName());
			m.put("item_pic", d.getItemPic());
			m.put("num", d.getNum());
			m.put("refund_fee", d.getRefundFee());
			m.put("refund_point", d.getRefundPoint());
			m.put("aftersales_type", d.getAftersalesType());
			m.put("progress", d.getProgress());
			m.put("aftersales_status", d.getAftersalesStatus());
			m.put("create_time", formatEpochSeconds(d.getCreateTime()));
			m.put("update_time", formatEpochSeconds(d.getCreateTime()));
			m.put("auto_refuse_time", formatAutoRefuseTime(d.getAutoRefuseTime()));
			out.add(m);
		}
		return out;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}

	private List<String> parseEvidencePic(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String trimmed = raw.trim();
		try {
			JsonNode node = objectMapper.readTree(trimmed);
			if (node.isArray()) {
				List<String> pics = new ArrayList<>();
				for (JsonNode item : node) {
					if (item.isTextual()) {
						pics.add(item.asText());
					} else if (!item.isNull()) {
						pics.add(item.asText());
					}
				}
				return pics;
			}
		} catch (JsonProcessingException ignored) {
			// fall through
		}
		return List.of();
	}

	private Map<String, Object> parseJsonObject(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String trimmed = raw.trim();
		if ("0".equals(trimmed)) {
			return null;
		}
		try {
			return objectMapper.readValue(trimmed, MAP_TYPE);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(0L));
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatAutoRefuseTime(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String trimmed = raw.trim();
		if ("0".equals(trimmed)) {
			return "";
		}
		try {
			long epoch = Long.parseLong(trimmed);
			if (epoch <= 0L) {
				return "";
			}
			return DATE_TIME_FMT.format(Instant.ofEpochSecond(epoch));
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private static boolean needDefaultAddress(String addr) {
		if (addr == null) {
			return true;
		}
		if (addr.isEmpty()) {
			return true;
		}
		return "0".equals(addr.trim());
	}
}
