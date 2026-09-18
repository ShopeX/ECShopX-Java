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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.cron.port.TurntableClearSurplusTimesRedisPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TurntableScheduleService {

	public record ScheduleClearResult(int operatorRowsPulled, int redisKeysDeleted) {}

	private final OperatorsMapper operatorsMapper;
	private final LuckyDrawActivityMapper luckyDrawActivityMapper;
	private final ObjectMapper objectMapper;
	private final TurntableClearSurplusTimesRedisPort turntableClearSurplusTimesRedisPort;

	/**
	 * 与 PHP 错位分页一致：第 2000 页、每页 1 条、按 created 降序；总账号 &lt; 2000 时通常无记录。
	 */
	public ScheduleClearResult scheduleClearTurntableTimesOver() {
		Page<Operators> page = new Page<>(2000, 1);
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.orderByDesc(Operators::getCreated);
		Page<Operators> out = operatorsMapper.selectPage(page, w);
		List<Operators> records = out.getRecords();
		if (records == null || records.isEmpty()) {
			return new ScheduleClearResult(0, 0);
		}
		int redisKeys = 0;
		int pulled = 0;
		for (Operators op : records) {
			Long cid = op.getCompanyId();
			if (cid == null || cid <= 0L) {
				continue;
			}
			pulled++;
			redisKeys += clearTurntableTimesOverForCompany(cid);
		}
		return new ScheduleClearResult(pulled, redisKeys);
	}

	/**
	 * 包可见便于同包单测；返回本 company 下是否实际发起 1 次整键删除。
	 */
	int clearTurntableTimesOverForCompany(long companyId) {
		LuckyDrawActivity row = luckyDrawActivityMapper.selectOne(
				new LambdaQueryWrapper<LuckyDrawActivity>()
						.eq(LuckyDrawActivity::getCompanyId, companyId)
						.orderByDesc(LuckyDrawActivity::getId)
						.last("LIMIT 1"));
		if (row == null) {
			return 0;
		}
		Map<String, Object> cfg = parseTemplateConfig(row.getActivityTemplateConfig());
		String longTerm = strOrDefault(cfg, "long_term", "0").trim();
		String clearAfter = strOrDefault(cfg, "clear_times_after_end", "0").trim();
		long endSec = row.getEndTime() == null ? 0L : row.getEndTime();
		long now = Instant.now().getEpochSecond();
		boolean notLongTerm = !"1".equals(longTerm);
		boolean wantClear = "1".equals(clearAfter);
		if (notLongTerm && wantClear && endSec < now) {
			turntableClearSurplusTimesRedisPort.deleteEntireSurplusTimesKey(companyId);
			return 1;
		}
		return 0;
	}

	private Map<String, Object> parseTemplateConfig(String json) {
		if (!StringUtils.hasText(json)) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			return Collections.emptyMap();
		}
	}

	private static String strOrDefault(Map<String, Object> m, String key, String def) {
		if (m == null) {
			return def;
		}
		Object v = m.get(key);
		if (v == null) {
			return def;
		}
		return String.valueOf(v);
	}
}
