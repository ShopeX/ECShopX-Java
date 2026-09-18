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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateModifyStatusRequest;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class PagesTemplateModifyStatusService {

	private static final ZoneId SHANGHAI_ZONE_ID = ZoneId.of("Asia/Shanghai");

	private final PagesTemplateMapper pagesTemplateMapper;

	public PagesTemplateModifyStatusService(PagesTemplateMapper pagesTemplateMapper) {
		this.pagesTemplateMapper = pagesTemplateMapper;
	}

	public Map<String, Object> modifyStatus(long companyId, PagesTemplateModifyStatusRequest request) {
		Integer requestStatus = request.getStatus();
		Integer requestTimerStatus = request.getTimerStatus();
		if (isLooseEmptyScalar(requestStatus) && isLooseEmptyScalar(requestTimerStatus)) {
			throw new ResourceException("无效的状态变更操作");
		}
		if (timerStatusIsLooselyOne(requestTimerStatus)) {
			if (isUnsetTimerTime(request.getTimerTime())) {
				throw new ResourceException("定时启用缺少必要参数");
			}
		}

		Long pagesTemplateId = request.getPagesTemplateId();
		if (pagesTemplateId == null || pagesTemplateId <= 0L) {
			throw new ResourceException("无效的模板");
		}

		PagesTemplate info =
				pagesTemplateMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
								.isNull(PagesTemplate::getDeletedAt));
		if (info == null) {
			throw new ResourceException("无效的模板");
		}

		Long regionauthId = info.getRegionauthId();
		int distributorId = info.getDistributorId() != null ? info.getDistributorId() : 0;
		String weappPages = info.getWeappPages() != null ? info.getWeappPages() : "index";

		if (looseEqualTwo(requestStatus)) {
			long countEnabled =
					pagesTemplateMapper.selectCount(
							new LambdaQueryWrapper<PagesTemplate>()
									.eq(PagesTemplate::getCompanyId, companyId)
									.eq(PagesTemplate::getRegionauthId, regionauthId)
									.eq(PagesTemplate::getDistributorId, distributorId)
									.eq(PagesTemplate::getWeappPages, weappPages)
									.eq(PagesTemplate::getStatus, 1)
									.isNull(PagesTemplate::getDeletedAt));
			if (countEnabled <= 1L && distributorId == 0) {
				throw new ResourceException("至少开启一套模版");
			}
		}

		if (looseEqualOne(requestTimerStatus)) {
			if (info.getStatus() != null && info.getStatus().intValue() == 1) {
				throw new ResourceException("当前模板已是开启状态，无需定时启用操作");
			}
			long countTimerOn =
					pagesTemplateMapper.selectCount(
							new LambdaQueryWrapper<PagesTemplate>()
									.eq(PagesTemplate::getCompanyId, companyId)
									.eq(PagesTemplate::getRegionauthId, regionauthId)
									.eq(PagesTemplate::getDistributorId, distributorId)
									.eq(PagesTemplate::getWeappPages, weappPages)
									.eq(PagesTemplate::getTimerStatus, 1)
									.isNull(PagesTemplate::getDeletedAt));
			if (countTimerOn >= 1L) {
				throw new ResourceException("已有启用的定时模版");
			}
		}

		int nowEpoch = 0;
		if (looseEqualOne(requestStatus)) {
			nowEpoch = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<PagesTemplate> bulk =
					new LambdaUpdateWrapper<PagesTemplate>()
							.set(PagesTemplate::getStatus, 2)
							.eq(PagesTemplate::getCompanyId, companyId)
							.eq(PagesTemplate::getRegionauthId, regionauthId)
							.eq(PagesTemplate::getDistributorId, distributorId)
							.eq(PagesTemplate::getWeappPages, weappPages)
							.eq(PagesTemplate::getStatus, 1)
							.isNull(PagesTemplate::getDeletedAt);
			pagesTemplateMapper.update(null, bulk);
		}

		LambdaUpdateWrapper<PagesTemplate> uw =
				new LambdaUpdateWrapper<PagesTemplate>()
						.set(
								PagesTemplate::getTimerTime,
								resolveTimerTimeEpochSecondsForUpdate(request.getTimerTime()));
		if (requestStatus != null && requestStatus.intValue() != 0) {
			uw.set(PagesTemplate::getStatus, requestStatus);
		}
		if (requestTimerStatus != null && requestTimerStatus.intValue() != 0) {
			uw.set(PagesTemplate::getTimerStatus, requestTimerStatus);
		}
		if (looseEqualOne(requestStatus)) {
			uw.set(PagesTemplate::getTemplateStatusModifyTime, nowEpoch);
		}
		uw.eq(PagesTemplate::getCompanyId, companyId)
				.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
				.isNull(PagesTemplate::getDeletedAt);

		int singleAffectedRows = pagesTemplateMapper.update(null, uw);
		if (singleAffectedRows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		return Map.of("status", Boolean.TRUE);
	}

	private static boolean isLooseEmptyScalar(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof Boolean b) {
			return !b;
		}
		if (value instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (value instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				return true;
			}
			return "0".equals(s);
		}
		return false;
	}

	private static boolean timerStatusIsLooselyOne(Integer timerStatus) {
		return !isLooseEmptyScalar(timerStatus) && timerStatus != null && timerStatus.intValue() == 1;
	}

	private static boolean isUnsetTimerTime(String timerTime) {
		return timerTime == null || !StringUtils.hasText(timerTime.trim());
	}

	private static boolean looseEqualOne(Integer v) {
		return v != null && v.intValue() == 1;
	}

	private static boolean looseEqualTwo(Integer v) {
		return v != null && v.intValue() == 2;
	}

	private int resolveTimerTimeEpochSecondsForUpdate(String timerTimeRaw) {
		if (timerTimeRaw == null || !StringUtils.hasText(timerTimeRaw.trim())) {
			return 0;
		}
		String s = timerTimeRaw.trim();
		DateTimeFormatter[] dateTimePatterns =
				new DateTimeFormatter[] {
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
					DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
					DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
				};
		for (DateTimeFormatter fmt : dateTimePatterns) {
			try {
				LocalDateTime ldt = LocalDateTime.parse(s, fmt);
				return (int) ldt.atZone(SHANGHAI_ZONE_ID).toEpochSecond();
			} catch (DateTimeParseException ignored) {
				// try next
			}
		}
		try {
			LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			return (int) ld.atStartOfDay(SHANGHAI_ZONE_ID).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			// continue
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return (int) ldt.atZone(SHANGHAI_ZONE_ID).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		return 0;
	}
}
