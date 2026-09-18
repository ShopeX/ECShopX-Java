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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.dispatch.RecordReviewNoticeDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Row-level import for registration audit Excel: updates record status and sends the same notifications as the admin
 * review API, without the short-lived Redis lock used by interactive review.
 */
@Service
public class RegistrationRecordImportReviewRowService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService;
	private final RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService;
	private final RecordReviewNoticeDispatchPublisher recordReviewNoticeDispatchPublisher;

	public RegistrationRecordImportReviewRowService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService,
			RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService,
			RecordReviewNoticeDispatchPublisher recordReviewNoticeDispatchPublisher) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordOutsideMultiLangWriteService = registrationRecordOutsideMultiLangWriteService;
		this.registrationRecordActivitySuccessService = registrationRecordActivitySuccessService;
		this.recordReviewNoticeDispatchPublisher = recordReviewNoticeDispatchPublisher;
	}

	public void acceptRow(long companyId, Map<String, Object> row) {
		String mobile = trimToEmpty(row.get("mobile"));
		String recordIdStr = trimToEmpty(row.get("record_id"));
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("请输入正确的手机号码");
		}
		if (!MOBILE_PATTERN.matcher(mobile).matches()) {
			throw new BadRequestException("请输入正确的手机号码");
		}
		if (!StringUtils.hasText(recordIdStr)) {
			throw new BadRequestException("请输入正确的报名申请编号");
		}
		long recordId;
		try {
			recordId = Long.parseLong(recordIdStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("请输入正确的报名申请编号");
		}

		RegistrationRecord rec =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getCompanyId, companyId)
								.eq(RegistrationRecord::getRecordId, recordId)
								.eq(RegistrationRecord::getMobile, mobile)
								.last("LIMIT 1"));
		if (rec == null) {
			try {
				acceptRowLegacyMobileMatch(companyId, recordId, mobile, row);
			} catch (ResourceException | BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("保存数据错误");
			}
			return;
		}
		try {
			applyReviewCore(companyId, rec, row);
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("保存数据错误");
		}
	}

	private void acceptRowLegacyMobileMatch(long companyId, long recordId, String mobile, Map<String, Object> row) {
		RegistrationRecord rec =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getCompanyId, companyId)
								.eq(RegistrationRecord::getRecordId, recordId)
								.last("LIMIT 1"));
		if (rec == null) {
			throw new ResourceException("报名记录不存在");
		}
		String stored = rec.getMobile() == null ? "" : rec.getMobile().trim();
		if (!stored.equals(mobile)) {
			String normalizedStored = normalizeMobileDigits(stored);
			String normalizedInput = normalizeMobileDigits(mobile);
			if (!normalizedInput.equals(normalizedStored)) {
				throw new ResourceException("报名记录不存在");
			}
		}
		applyReviewCore(companyId, rec, row);
	}

	private static String normalizeMobileDigits(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim();
		int n = s.length();
		if (n <= 11) {
			return s;
		}
		return s.substring(n - 11);
	}

	private void applyReviewCore(long companyId, RegistrationRecord rec, Map<String, Object> row) {
		if (!"pending".equals(rec.getStatus())) {
			return;
		}
		boolean passed = parsePassed(row.get("review_result"));
		String newStatus = passed ? "passed" : "rejected";
		String reason = row.get("reason") == null ? "" : String.valueOf(row.get("reason")).trim();

		RegistrationRecord snapshot = new RegistrationRecord();
		snapshot.setRecordId(rec.getRecordId());
		snapshot.setCompanyId(rec.getCompanyId());
		snapshot.setActivityId(rec.getActivityId());
		snapshot.setUserId(rec.getUserId());
		snapshot.setGetPoints(rec.getGetPoints());
		snapshot.setIsWhiteList(rec.getIsWhiteList());
		snapshot.setMobile(rec.getMobile());
		snapshot.setFormMobile(rec.getFormMobile());
		snapshot.setTrueName(rec.getTrueName());

		int now = (int) Instant.now().getEpochSecond();
		rec.setStatus(newStatus);
		rec.setReason(reason);
		rec.setUpdated(now);
		int u = registrationRecordMapper.updateById(rec);
		if (u == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> lang = new LinkedHashMap<>();
		lang.put("reason", reason);
		registrationRecordOutsideMultiLangWriteService.updateLangFields(rec.getRecordId(), companyId, lang, "zh-CN");

		if ("passed".equals(newStatus)) {
			RegistrationActivity act =
					registrationActivityMapper.selectOne(
							new LambdaQueryWrapper<RegistrationActivity>()
									.eq(RegistrationActivity::getCompanyId, companyId)
									.eq(RegistrationActivity::getActivityId, snapshot.getActivityId())
									.last("LIMIT 1"));
			if (act == null) {
				throw new ResourceException("未查询到更新数据");
			}
			registrationRecordActivitySuccessService.activitySuccess(snapshot, act, Locale.SIMPLIFIED_CHINESE);
		}

		recordReviewNoticeDispatchPublisher.publish(companyId, rec.getRecordId());
	}

	private static boolean parsePassed(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			return Integer.parseInt(s) == 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static String trimToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
