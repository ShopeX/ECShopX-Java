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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.OperatorDataPass;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class OperatorDataPassApplyService {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZONE);

	private final OperatorDataPassMapper operatorDataPassMapper;
	private final OperatorsQueryService operatorsQueryService;

	public OperatorDataPassApplyService(
			OperatorDataPassMapper operatorDataPassMapper, OperatorsQueryService operatorsQueryService) {
		this.operatorDataPassMapper = operatorDataPassMapper;
		this.operatorsQueryService = operatorsQueryService;
	}

	/**
	 * @return {@code null} if insert succeeded; otherwise conflict HTML message (HTTP 200, status false).
	 */
	@Transactional(rollbackFor = Exception.class)
	public String apply(
			long companyId,
			int operatorId,
			int startTimeSec,
			int endTimeSec,
			int dateType,
			String rangeNormalizedOrNull,
			String reason) {
		if (dateType != 0) {
			int startDayOfWeekSundayZero = dayOfWeekSundayZero(startTimeSec);
			int span = endTimeSec - startTimeSec;
			if ((startDayOfWeekSundayZero == 6 && span <= 86400)
					|| (startDayOfWeekSundayZero == 0 && span <= 0)) {
				throw new ResourceException("日期存在错误");
			}
		}

		List<OperatorDataPass> overlaps = searchOverlapping(companyId, operatorId, startTimeSec, endTimeSec);
		List<String> errTime1 = new ArrayList<>();
		List<String> errTime2 = new ArrayList<>();
		boolean appRange = rangeNormalizedOrNull != null && !rangeNormalizedOrNull.isEmpty();

		for (OperatorDataPass p : overlaps) {
			OperatorDataPassRuleCodec.TimeWeek tw = OperatorDataPassRuleCodec.splitTimeAndWeek(p.getRule());
			String time = tw.time();
			String week = tw.week();

			int overlapStart = Math.max(startTimeSec, p.getStartTime());
			int overlapEnd = Math.min(endTimeSec, p.getEndTime());

			if ((dateType != 0 || !"*".equals(week))
					&& (weekendOverlapSkip(overlapStart, overlapEnd))) {
				continue;
			}

			String timeRange;
			if (appRange && !"*".equals(time)) {
				String[] pSeg = time.split("-", 2);
				String[] aSeg = rangeNormalizedOrNull.split("-", 2);
				if (pSeg.length != 2 || aSeg.length != 2) {
					continue;
				}
				String pStart = pSeg[0].trim();
				String pEnd = pSeg[1].trim();
				String aStart = aSeg[0].trim();
				String aEnd = aSeg[1].trim();
				String startH = maxStr(pStart, aStart);
				String endH = minStr(pEnd, aEnd);
				if (startH.compareTo(endH) > 0) {
					continue;
				}
				timeRange = startH + "至" + endH;
			} else {
				if ("*".equals(time) && appRange) {
					String[] aSeg = rangeNormalizedOrNull.split("-", 2);
					if (aSeg.length != 2) {
						timeRange = "全天";
					} else {
						timeRange = aSeg[0].trim() + "至" + aSeg[1].trim();
					}
				} else if (!appRange && !"*".equals(time)) {
					String[] pSeg = time.split("-", 2);
					if (pSeg.length != 2) {
						timeRange = "全天";
					} else {
						timeRange = pSeg[0].trim() + "至" + pSeg[1].trim();
					}
				} else {
					timeRange = "全天";
				}
			}

			String startDate = YMD.format(Instant.ofEpochSecond(overlapStart));
			String endDate = YMD.format(Instant.ofEpochSecond(overlapEnd));
			String fragment = "<br />重复时间：<br />" + startDate + " 至 " + endDate + "<br />" + timeRange;
			if (p.getStatus() != null && p.getStatus() == 0) {
				errTime1.add(fragment);
			} else {
				errTime2.add(fragment);
			}
		}

		String errInfo = "";
		if (!errTime1.isEmpty()) {
			errInfo = "申请权限开通时间重复，请核实后再试。<br />" + String.join("", errTime1);
		}
		if (!errTime2.isEmpty()) {
			if (!errInfo.isEmpty()) {
				errInfo += "<br /><br />";
			}
			errInfo += "该时间段权限已开通，请核实后再试。<br />" + String.join("", errTime2);
		}
		if (!errInfo.isEmpty()) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return errInfo;
		}

		Map<String, Object> opInfo = operatorsQueryService.getInfo(Map.of("operator_id", operatorId));
		if (opInfo == null || opInfo.isEmpty()) {
			throw new ResourceException("账号不存在");
		}
		long merchantId = parsePositiveMerchantId(opInfo.get("merchant_id"));

		OperatorDataPass row = new OperatorDataPass();
		row.setCompanyId(companyId);
		row.setOperatorId(operatorId);
		row.setStatus(0);
		row.setStartTime(startTimeSec);
		row.setEndTime(endTimeSec);
		row.setRule(OperatorDataPassRuleCodec.transToRule(rangeNormalizedOrNull, dateType));
		row.setReason(reason != null ? reason : "");
		row.setRemarks("");
		row.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		row.setApproveTime(0);
		row.setIsClosed(0);
		row.setMerchantId(merchantId);
		operatorDataPassMapper.insert(row);
		return null;
	}

	private static long parsePositiveMerchantId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String maxStr(String a, String b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	private static String minStr(String a, String b) {
		return a.compareTo(b) <= 0 ? a : b;
	}

	/** Sunday = 0, Monday = 1, … Saturday = 6 (matches common calendar weekday indexing). */
	private static int dayOfWeekSundayZero(int epochSec) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZONE);
		int isoDow = zdt.getDayOfWeek().getValue();
		return isoDow == 7 ? 0 : isoDow;
	}

	private static boolean weekendOverlapSkip(int overlapStart, int overlapEnd) {
		int overlapStartDowSundayZero = dayOfWeekSundayZero(overlapStart);
		int span = overlapEnd - overlapStart;
		return (overlapStartDowSundayZero == 6 && span <= 86400)
				|| (overlapStartDowSundayZero == 0 && span <= 0);
	}

	private List<OperatorDataPass> searchOverlapping(long companyId, int operatorId, int appStart, int appEnd) {
		return operatorDataPassMapper.selectList(new LambdaQueryWrapper<OperatorDataPass>()
				.eq(OperatorDataPass::getCompanyId, companyId)
				.eq(OperatorDataPass::getOperatorId, operatorId)
				.ne(OperatorDataPass::getStatus, 2)
				.and(w -> w.nested(n -> n.le(OperatorDataPass::getStartTime, appStart)
								.ge(OperatorDataPass::getEndTime, appStart))
						.or(n -> n.le(OperatorDataPass::getStartTime, appEnd)
								.ge(OperatorDataPass::getEndTime, appEnd))
						.or(n -> n.ge(OperatorDataPass::getStartTime, appStart)
								.le(OperatorDataPass::getStartTime, appEnd))
						.or(n -> n.ge(OperatorDataPass::getEndTime, appStart)
								.le(OperatorDataPass::getEndTime, appEnd))));
	}
}
