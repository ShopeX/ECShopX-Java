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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OperatorDataPassExPassFormatter {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZONE);

	public void applyExPassItemToMap(OperatorDataPass entity, LinkedHashMap<String, Object> target) {
		OperatorDataPassRuleCodec.RuleParts parts = OperatorDataPassRuleCodec.parseStoredRule(entity.getRule());
		String rangeOut = parts.range();
		int dateType = parts.dateType();

		long startSec = Optional.ofNullable(entity.getStartTime()).map(Integer::longValue).orElse(0L);
		long endSec = Optional.ofNullable(entity.getEndTime()).map(Integer::longValue).orElse(0L);
		String startDay = YMD.format(Instant.ofEpochSecond(startSec));
		String endDay = YMD.format(Instant.ofEpochSecond(endSec));

		String exDataType = dateType != 0 ? "每周一到周五" : "每天";
		String exRange = "";
		if (StringUtils.hasText(rangeOut)) {
			exRange = " " + rangeOut.replace("-", " 到 ") + " ";
		}
		String ex = exDataType + exRange + "有权限，生效时间：" + startDay + "，结束时间：" + endDay;

		target.put("range", rangeOut);
		target.put("date_type", dateType);
		target.put("start_time", startDay);
		target.put("end_time", endDay);
		target.put("ex", ex);
	}
}
