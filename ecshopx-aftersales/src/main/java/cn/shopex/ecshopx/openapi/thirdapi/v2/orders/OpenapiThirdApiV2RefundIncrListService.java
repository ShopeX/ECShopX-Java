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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
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
public class OpenapiThirdApiV2RefundIncrListService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[345789][0-9]{9}$");
	private static final DateTimeFormatter LOCAL_DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final MemberAccountService memberAccountService;
	private final OpenapiThirdApiV2RefundListFormatSupport refundListFormatSupport;

	public OpenapiThirdApiV2RefundIncrListService(
			AftersalesRefundMapper aftersalesRefundMapper,
			MemberAccountService memberAccountService,
			OpenapiThirdApiV2RefundListFormatSupport refundListFormatSupport) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.memberAccountService = memberAccountService;
		this.refundListFormatSupport = refundListFormatSupport;
	}

	public Map<String, Object> incrList(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean startModifiedTruthy,
			String startModifiedRaw,
			boolean endModifiedTruthy,
			String endModifiedRaw) {
		IncrRefundFilter filter = new IncrRefundFilter();
		filter.companyId = companyId;

		if (mobileTruthy) {
			String mobile = mobileRaw != null ? mobileRaw.trim() : "";
			if (!MOBILE_PATTERN.matcher(mobile).matches()) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_REFUND_HANDLE_ERROR, "请填写正确的手机号");
			}
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
			if (member == null) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_MEMBER_NOT_FOUND, "会员找不到");
			}
			filter.userId = member.getUserId();
		}

		if (startModifiedTruthy && endModifiedTruthy) {
			long startEpoch = phpStrtotime(startModifiedRaw);
			long endEpoch = phpStrtotime(endModifiedRaw);
			if (startEpoch > endEpoch) {
				throw new OpenapiAftersalesV2FailException(
						OpenapiErrorCode.ORDER_REFUND_HANDLE_ERROR, "开始时间不能大于结束时间");
			}
		}
		if (startModifiedTruthy) {
			filter.updateTimeGte = toEpochSecondsInt(phpStrtotime(startModifiedRaw));
		}
		if (endModifiedTruthy) {
			filter.updateTimeLte = toEpochSecondsInt(phpStrtotime(endModifiedRaw));
		}

		LambdaQueryWrapper<AftersalesRefund> wrapper = buildIncrWrapper(filter);
		long totalCount = aftersalesRefundMapper.selectCount(wrapper);

		LambdaQueryWrapper<AftersalesRefund> listWrapper = wrapper.clone();
		long offset = (long) (page - 1) * pageSize;
		listWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<AftersalesRefund> entities = aftersalesRefundMapper.selectList(listWrapper);

		List<Map<String, Object>> formattedRows =
				entities.stream().map(refundListFormatSupport::formatOpenApiRefundRow).toList();
		return refundListFormatSupport.formatListStruct(totalCount, formattedRows, page, pageSize);
	}

	private LambdaQueryWrapper<AftersalesRefund> buildIncrWrapper(IncrRefundFilter filter) {
		LambdaQueryWrapper<AftersalesRefund> wrapper =
				new LambdaQueryWrapper<AftersalesRefund>().eq(AftersalesRefund::getCompanyId, filter.companyId);

		if (filter.userId != null) {
			wrapper.eq(AftersalesRefund::getUserId, filter.userId);
		}
		if (filter.updateTimeGte != null) {
			wrapper.ge(AftersalesRefund::getUpdateTime, filter.updateTimeGte);
		}
		if (filter.updateTimeLte != null) {
			wrapper.le(AftersalesRefund::getUpdateTime, filter.updateTimeLte);
		}

		return wrapper.orderByDesc(AftersalesRefund::getUpdateTime);
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

	private static final class IncrRefundFilter {
		private long companyId;
		private Long userId;
		private Integer updateTimeGte;
		private Integer updateTimeLte;
	}
}
