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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2AftersalesListService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[345789][0-9]{9}$");
	private static final DateTimeFormatter LOCAL_DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final AftersalesMapper aftersalesMapper;
	private final MemberAccountService memberAccountService;
	private final OpenapiThirdApiV2AftersalesListFormatSupport formatSupport;

	public OpenapiThirdApiV2AftersalesListService(
			AftersalesMapper aftersalesMapper,
			MemberAccountService memberAccountService,
			OpenapiThirdApiV2AftersalesListFormatSupport formatSupport) {
		this.aftersalesMapper = aftersalesMapper;
		this.memberAccountService = memberAccountService;
		this.formatSupport = formatSupport;
	}

	public Map<String, Object> list(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean timeBeginTruthy,
			String timeBeginRaw,
			boolean timeEndTruthy,
			String timeEndRaw) {
		AftersalesFilter filter = new AftersalesFilter();
		filter.companyId = companyId;

		if (mobileTruthy) {
			String mobile = mobileRaw != null ? mobileRaw.trim() : "";
			if (!MOBILE_PATTERN.matcher(mobile).matches()) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_AFTERSALES_HANDLE_ERROR, "请填写正确的手机号");
			}
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
			if (member == null) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_MEMBER_NOT_FOUND, "会员找不到");
			}
			filter.userId = member.getUserId();
		}

		if (timeBeginTruthy && timeEndTruthy) {
			long beginEpoch = phpStrtotime(timeBeginRaw);
			long endEpoch = phpStrtotime(timeEndRaw);
			if (beginEpoch > endEpoch) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_AFTERSALES_HANDLE_ERROR, "开始时间不能大于结束时间");
			}
		}
		if (timeBeginTruthy) {
			filter.createTimeGte = toEpochSecondsInt(phpStrtotime(timeBeginRaw));
		}
		if (timeEndTruthy) {
			filter.createTimeLte = toEpochSecondsInt(phpStrtotime(timeEndRaw));
		}

		LambdaQueryWrapper<Aftersales> wrapper = buildWrapper(filter);
		long totalCount = aftersalesMapper.selectCount(wrapper);

		LambdaQueryWrapper<Aftersales> listWrapper = wrapper.clone();
		long offset = (long) (page - 1) * pageSize;
		listWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<Aftersales> entities = aftersalesMapper.selectList(listWrapper);

		List<OpenapiThirdApiV2AftersalesListFormatSupport.EnrichedRow> enrichedRows =
				entities.isEmpty() ? List.of() : formatSupport.enrichRows(entities, companyId);
		List<Map<String, Object>> formattedRows =
				enrichedRows.stream().map(formatSupport::formatOpenApiAftersalesRow).toList();
		return formatSupport.formatListStruct(totalCount, formattedRows, page, pageSize);
	}

	private LambdaQueryWrapper<Aftersales> buildWrapper(AftersalesFilter filter) {
		LambdaQueryWrapper<Aftersales> wrapper =
				new LambdaQueryWrapper<Aftersales>().eq(Aftersales::getCompanyId, filter.companyId);

		if (filter.userId != null) {
			wrapper.eq(Aftersales::getUserId, filter.userId);
		}
		if (filter.createTimeGte != null) {
			wrapper.ge(Aftersales::getCreateTime, filter.createTimeGte);
		}
		if (filter.createTimeLte != null) {
			wrapper.le(Aftersales::getCreateTime, filter.createTimeLte);
		}

		return wrapper.orderByDesc(Aftersales::getCreateTime);
	}

	private static long phpStrtotime(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		String t = raw.trim();
		try {
			LocalDateTime dt = LocalDateTime.parse(t, LOCAL_DATE_TIME_FMT);
			return dt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		try {
			LocalDate d = LocalDate.parse(t, DATE_FMT);
			return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			return 0L;
		}
	}

	private static int toEpochSecondsInt(long epochSeconds) {
		return (int) epochSeconds;
	}

	private static final class AftersalesFilter {
		private long companyId;
		private Long userId;
		private Integer createTimeGte;
		private Integer createTimeLte;
	}
}
