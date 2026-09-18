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

package cn.shopex.ecshopx.promotions.service.checkin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.CheckInLog;
import cn.shopex.ecshopx.promotions.mapper.CheckInLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInFrontCreateService {

	private static final ZoneId CHECKIN_ZONE = ZoneId.of("Asia/Shanghai");

	private final CheckInLogMapper checkInLogMapper;
	private final MessageSource messageSource;

	public CheckInFrontCreateService(CheckInLogMapper checkInLogMapper, MessageSource messageSource) {
		this.checkInLogMapper = checkInLogMapper;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createCheckIn(long companyId, long userId, String checkDayYmd, Locale locale) {
		int dayYmd = Integer.parseInt(checkDayYmd);

		LambdaQueryWrapper<CheckInLog> dup = new LambdaQueryWrapper<CheckInLog>()
				.eq(CheckInLog::getCompanyId, companyId)
				.eq(CheckInLog::getUserId, userId)
				.eq(CheckInLog::getCreateTime, dayYmd)
				.last("LIMIT 1");
		CheckInLog existing = checkInLogMapper.selectOne(dup);
		if (existing != null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.checkin.already_signed_in_no_repeat", null, locale));
		}

		LocalDate today = LocalDate.now(CHECKIN_ZONE);
		LocalDate first = today.withDayOfMonth(1);
		LocalDate last = first.with(TemporalAdjusters.lastDayOfMonth());
		int startYmd = yyyyMmddInt(first);
		int endYmd = yyyyMmddInt(last);

		LambdaQueryWrapper<CheckInLog> w = new LambdaQueryWrapper<CheckInLog>()
				.eq(CheckInLog::getCompanyId, companyId)
				.eq(CheckInLog::getUserId, userId)
				.ge(CheckInLog::getCreateTime, startYmd)
				.le(CheckInLog::getCreateTime, endYmd);
		Long total = checkInLogMapper.selectCount(w);
		int nextTag = (int) ((total == null ? 0L : total.longValue()) + 1L);

		CheckInLog entity = new CheckInLog();
		entity.setCompanyId(companyId);
		entity.setUserId(userId);
		entity.setCreateTime(dayYmd);
		entity.setTag(String.valueOf(nextTag));
		int nowSec = (int) (System.currentTimeMillis() / 1000);
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);
		checkInLogMapper.insert(entity);

		LambdaQueryWrapper<CheckInLog> afterInsert = new LambdaQueryWrapper<CheckInLog>()
				.eq(CheckInLog::getCompanyId, companyId)
				.eq(CheckInLog::getUserId, userId)
				.eq(CheckInLog::getCreateTime, dayYmd)
				.last("LIMIT 1");
		CheckInLog row = checkInLogMapper.selectOne(afterInsert);
		if (row == null) {
			throw new ResourceException(messageSource.getMessage("promotions.checkin.persist_failed", null, locale));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId().intValue());
		out.put("company_id", row.getCompanyId().intValue());
		out.put("user_id", row.getUserId().intValue());
		out.put("create_time", row.getCreateTime());
		out.put("tag", row.getTag());
		out.put("created", row.getCreated());
		return out;
	}

	private static int yyyyMmddInt(LocalDate d) {
		return d.getYear() * 10_000 + d.getMonthValue() * 100 + d.getDayOfMonth();
	}
}
