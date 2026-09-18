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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.RightsOperateLogs;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsOperateLogsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RightsAdminDelayRightsService {

	private final RightsMapper rightsMapper;
	private final RightsOperateLogsMapper rightsOperateLogsMapper;

	public RightsAdminDelayRightsService(
			RightsMapper rightsMapper, RightsOperateLogsMapper rightsOperateLogsMapper) {
		this.rightsMapper = rightsMapper;
		this.rightsOperateLogsMapper = rightsOperateLogsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> delayRights(
			long companyId,
			long rightsId,
			String delayDateRaw,
			String remark,
			Long operatorId) {
		Rights detail = rightsMapper.selectById(rightsId);
		if (detail == null) {
			throw new ResourceException("rights_id=" + rightsId + "的权益不存在");
		}

		int delayDateEpochSec = resolveDelayEpochSeconds(delayDateRaw);

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Rights> uw =
				Wrappers.<Rights>lambdaUpdate()
						.eq(Rights::getRightsId, rightsId)
						.eq(Rights::getCompanyId, companyId)
						.set(Rights::getEndTime, delayDateEpochSec)
						.set(Rights::getStatus, "valid")
						.set(Rights::getUpdated, now);

		int rows = rightsMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("权益不存在");
		}
		if (rows != 1) {
			throw new ResourceException("更新权益结束日期失败");
		}

		RightsOperateLogs log = new RightsOperateLogs();
		log.setRightsId(rightsId);
		log.setCompanyId(companyId);
		log.setRemark(remark);
		log.setUserId(detail.getUserId());
		log.setOperatorId(operatorId);
		log.setOperator(null);
		log.setOriginalDate(detail.getEndTime());
		log.setDelayDate(delayDateEpochSec);
		log.setCreated(now);
		log.setUpdated(null);

		rightsOperateLogsMapper.insert(log);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", log.getId());
		out.put("rights_id", log.getRightsId());
		out.put("user_id", log.getUserId());
		out.put("company_id", log.getCompanyId());
		out.put("remark", log.getRemark());
		out.put("operator_id", log.getOperatorId());
		out.put("operator", log.getOperator());
		out.put("original_date", log.getOriginalDate());
		out.put("delay_date", log.getDelayDate());
		out.put("created", log.getCreated());
		out.put("updated", log.getUpdated());
		return out;
	}

	private static int resolveDelayEpochSeconds(String delayDateRaw) {
		try {
			long epoch = parseDelayDateToEpochSeconds(delayDateRaw);
			return Math.toIntExact(epoch);
		} catch (IllegalArgumentException | ArithmeticException ignored) {
			return 0;
		}
	}

	private static long parseDelayDateToEpochSeconds(String raw) {
		String s = raw.trim();
		if (!StringUtils.hasText(s)) {
			throw new IllegalArgumentException();
		}
		if (isNumericEpochString(s)) {
			long sec = Long.parseLong(s);
			if (sec < Integer.MIN_VALUE || sec > Integer.MAX_VALUE) {
				throw new IllegalArgumentException();
			}
			return sec;
		}
		try {
			LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
			long sec = d.atStartOfDay(ZoneId.of("Asia/Shanghai")).toEpochSecond();
			if (sec < Integer.MIN_VALUE || sec > Integer.MAX_VALUE) {
				throw new IllegalArgumentException();
			}
			return sec;
		} catch (DateTimeParseException ignored) {
			// try ISO instant
		}
		try {
			Instant inst = Instant.parse(s);
			long sec = inst.getEpochSecond();
			if (sec < Integer.MIN_VALUE || sec > Integer.MAX_VALUE) {
				throw new IllegalArgumentException();
			}
			return sec;
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static boolean isNumericEpochString(String s) {
		if (s.isEmpty()) {
			return false;
		}
		int i = 0;
		if (s.charAt(0) == '-') {
			if (s.length() == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
