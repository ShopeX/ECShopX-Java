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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.aftersales.domain.dto.DeliveryStaffAftersalesAggregateRow;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.dto.DeliveryStaffOrderAggregateRow;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontDeliveryStaffDataService {

	private static final Logger log = LoggerFactory.getLogger(FrontDeliveryStaffDataService.class);

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final SelfDeliveryStaffDistributorIdsResolver distributorIdsResolver;
	private final NormalOrdersMapper normalOrdersMapper;
	private final AftersalesMapper aftersalesMapper;
	private final ObjectMapper objectMapper;

	public FrontDeliveryStaffDataService(
			SelfDeliveryStaffDistributorIdsResolver distributorIdsResolver,
			NormalOrdersMapper normalOrdersMapper,
			AftersalesMapper aftersalesMapper,
			ObjectMapper objectMapper) {
		this.distributorIdsResolver = distributorIdsResolver;
		this.normalOrdersMapper = normalOrdersMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.objectMapper = objectMapper;
	}

	public ApiResult<Map<String, Object>> getDeliveryStaffData(
			long companyId,
			List<Long> operatorIds,
			String datetype,
			String date,
			Long distributorId) {
		List<Long> opIds = operatorIds == null ? List.of() : operatorIds;
		List<Long> distributorIds;
		if (distributorId != null && distributorId > 0) {
			distributorIds = List.of(distributorId);
		} else {
			distributorIds = distributorIdsResolver.resolve(companyId, opIds);
		}

		long startEpoch;
		long endEpoch;
		if (!StringUtils.hasText(datetype)) {
			throw new BadRequestException("缺少必填字段: datetype");
		}
		String dt = datetype.trim();
		if (!StringUtils.hasText(date)) {
			throw new BadRequestException("缺少 date 参数，请与 datetype 配对传入");
		}
		String dateVal = date.trim();
		switch (dt) {
			case "y" -> {
				int year;
				try {
					year = Integer.parseInt(dateVal);
				} catch (NumberFormatException e) {
					throw new BadRequestException("date 与 datetype=y 不匹配，应为整数年份");
				}
				Year y = Year.of(year);
				LocalDate first = y.atDay(1);
				LocalDate last = y.atMonth(12).atEndOfMonth();
				startEpoch = first.atStartOfDay(SHANGHAI).toEpochSecond();
				endEpoch = LocalDateTime.of(last, java.time.LocalTime.of(23, 59, 59)).atZone(SHANGHAI).toEpochSecond();
			}
			case "m" -> {
				YearMonth ym;
				try {
					ym = YearMonth.parse(dateVal, DateTimeFormatter.ofPattern("yyyy-MM"));
				} catch (DateTimeParseException e) {
					throw new BadRequestException("date 与 datetype=m 不匹配，应为 yyyy-MM 格式");
				}
				LocalDate first = ym.atDay(1);
				LocalDate last = ym.atEndOfMonth();
				startEpoch = first.atStartOfDay(SHANGHAI).toEpochSecond();
				endEpoch = LocalDateTime.of(last, java.time.LocalTime.of(23, 59, 59)).atZone(SHANGHAI).toEpochSecond();
			}
			case "d" -> {
				LocalDate day = parseDay(dateVal);
				startEpoch = day.atStartOfDay(SHANGHAI).toEpochSecond();
				endEpoch = LocalDateTime.of(day, java.time.LocalTime.of(23, 59, 59)).atZone(SHANGHAI).toEpochSecond();
			}
			default -> throw new BadRequestException("缺少或非法的 datetype 参数，有效取值为 y、m、d");
		}
		if (startEpoch > endEpoch) {
			throw new ResourceException("结束日期要大于等于开始日期");
		}

		Map<String, Object> oFilterLog = new LinkedHashMap<>();
		oFilterLog.put("self_delivery_status", "DONE");
		oFilterLog.put("company_id", companyId);
		oFilterLog.put("self_delivery_end_time|gte", startEpoch);
		oFilterLog.put("self_delivery_end_time|lte", endEpoch);
		oFilterLog.put("self_delivery_operator_id", opIds);
		if (!distributorIds.isEmpty()) {
			oFilterLog.put("distributor_id", distributorIds);
		}
		logOFilter(oFilterLog);

		Map<String, Object> aFilterLog = new LinkedHashMap<>();
		aFilterLog.put("aftersales_status", 2);
		aFilterLog.put("company_id", companyId);
		aFilterLog.put("self_delivery_operator_id", opIds);
		if (!distributorIds.isEmpty()) {
			aFilterLog.put("distributor_id", distributorIds);
		}
		logAFilter(aFilterLog);

		Map<String, Object> rFilterLog = new LinkedHashMap<>();
		rFilterLog.put("refund_success_time|gte", startEpoch);
		rFilterLog.put("refund_success_time|lte", endEpoch);
		logRFilter(rFilterLog);

		List<DeliveryStaffOrderAggregateRow> orderRows =
				normalOrdersMapper.selectDeliveryStaffAggregates(companyId, startEpoch, endEpoch, opIds, distributorIds, null);
		long orderCount = 0L;
		BigDecimal totalFeeCount = BigDecimal.ZERO;
		BigDecimal selfDeliveryFeeCount = BigDecimal.ZERO;
		for (DeliveryStaffOrderAggregateRow row : orderRows) {
			if (row.getOrderCount() != null) {
				orderCount += row.getOrderCount();
			}
			totalFeeCount = totalFeeCount.add(nz(row.getTotalFeeCount()));
			selfDeliveryFeeCount = selfDeliveryFeeCount.add(nz(row.getSelfDeliveryFeeCount()));
		}

		List<DeliveryStaffAftersalesAggregateRow> afterRows = aftersalesMapper.selectDeliveryStaffAftersalesAggregates(
				companyId, startEpoch, endEpoch, opIds, distributorIds);
		long aftersalesCount = 0L;
		BigDecimal refundFeeCount = BigDecimal.ZERO;
		for (DeliveryStaffAftersalesAggregateRow row : afterRows) {
			if (row.getAftersalesCount() != null) {
				aftersalesCount += row.getAftersalesCount();
			}
			refundFeeCount = refundFeeCount.add(nz(row.getRefundFeeCount()));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("order_count", orderCount);
		data.put("total_fee_count", totalFeeCount);
		data.put("self_delivery_fee_count", selfDeliveryFeeCount);
		data.put("aftersales_count", aftersalesCount);
		data.put("refund_fee_count", refundFeeCount);
		return ApiResult.ok(data);
	}

	private static LocalDate parseDay(String dateVal) {
		try {
			return LocalDate.parse(dateVal, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException ignored) {
			// continue
		}
		DateTimeFormatter[] formatters = {
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ofPattern("yyyy/M/d"),
			DateTimeFormatter.ofPattern("yyyy.M.d")
		};
		for (DateTimeFormatter f : formatters) {
			try {
				return LocalDate.parse(dateVal, f);
			} catch (DateTimeParseException ignored) {
				// try next
			}
		}
		throw new BadRequestException("date 与 datetype=d 不匹配，无法解析为有效日期");
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}

	private void logOFilter(Map<String, Object> oFilter) {
		try {
			log.info("getDeliveryStaffData oFilter:{}", objectMapper.writeValueAsString(oFilter));
		} catch (JsonProcessingException e) {
			log.info("getDeliveryStaffData oFilter:{}", oFilter);
		}
	}

	private void logAFilter(Map<String, Object> aFilter) {
		try {
			log.info("getDeliveryStaffData aFilter:{}", objectMapper.writeValueAsString(aFilter));
		} catch (JsonProcessingException e) {
			log.info("getDeliveryStaffData aFilter:{}", aFilter);
		}
	}

	private void logRFilter(Map<String, Object> rFilter) {
		try {
			log.info("getDeliveryStaffData rFilter:{}", objectMapper.writeValueAsString(rFilter));
		} catch (JsonProcessingException e) {
			log.info("getDeliveryStaffData rFilter:{}", rFilter);
		}
	}
}
