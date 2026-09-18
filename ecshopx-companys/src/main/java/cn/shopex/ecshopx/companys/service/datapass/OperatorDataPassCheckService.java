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

import cn.shopex.ecshopx.companys.domain.OperatorDataPass;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperatorDataPassCheckService {

	private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

	private final OperatorDataPassMapper operatorDataPassMapper;

	public OperatorDataPassCheckService(OperatorDataPassMapper operatorDataPassMapper) {
		this.operatorDataPassMapper = operatorDataPassMapper;
	}

	public boolean check(long companyId, long operatorId) {
		long now = Instant.now().getEpochSecond();
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(now), zone);
		long todayStart = zdt.toLocalDate().atStartOfDay(zone).toEpochSecond();

		List<OperatorDataPass> list = operatorDataPassMapper.selectList(new LambdaQueryWrapper<OperatorDataPass>()
				.eq(OperatorDataPass::getCompanyId, companyId)
				.eq(OperatorDataPass::getOperatorId, (int) operatorId)
				.le(OperatorDataPass::getStartTime, (int) todayStart)
				.ge(OperatorDataPass::getEndTime, (int) todayStart)
				.eq(OperatorDataPass::getStatus, 1)
				.eq(OperatorDataPass::getIsClosed, 0));

		int javaDow = zdt.getDayOfWeek().getValue();
		int dayOfWeekSundayZero = javaDow == 7 ? 0 : javaDow;
		String rn = HM.format(zdt.toLocalTime());

		for (OperatorDataPass p : list) {
			OperatorDataPassRuleCodec.RuleParts parts = OperatorDataPassRuleCodec.parseStoredRule(p.getRule());
			if (parts.dateType() == 1) {
				if (dayOfWeekSundayZero == 0 || dayOfWeekSundayZero == 6) {
					continue;
				}
			}
			if (!parts.range().isEmpty()) {
				String[] se = parts.range().split("-", 2);
				if (se.length == 2) {
					String rStart = se[0].trim();
					String rEnd = se[1].trim();
					if (rn.compareTo(rStart) < 0 || rn.compareTo(rEnd) > 0) {
						continue;
					}
				}
			}
			return true;
		}
		return false;
	}
}
