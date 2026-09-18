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

package cn.shopex.ecshopx.companys.service.statistics;

import cn.shopex.ecshopx.companys.domain.Statistics;
import cn.shopex.ecshopx.companys.mapper.StatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanysMemberStatisticsService {

	private static final DateTimeFormatter YMD_BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	private final StatisticsMapper statisticsMapper;
	private final StringRedisTemplate companysRedis;

	public CompanysMemberStatisticsService(
			StatisticsMapper statisticsMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedis) {
		this.statisticsMapper = statisticsMapper;
		this.companysRedis = companysRedis;
	}

	public LinkedHashMap<String, LinkedHashMap<String, Integer>> getMemberStatistics(long companyId, String todayYmd) {
		LocalDate today = LocalDate.parse(todayYmd, YMD_BASIC);
		LocalDate yesterday = today.minusDays(1);
		int yesterdayInt = Integer.parseInt(yesterday.format(YMD_BASIC));
		LocalDate sixDayBefore = today.minusDays(6);
		int sixDayBeforeInt = Integer.parseInt(sixDayBefore.format(YMD_BASIC));

		LinkedHashMap<String, LinkedHashMap<String, Integer>> byDate = new LinkedHashMap<>();
		for (int i = 0; i < 6; i++) {
			LocalDate d = sixDayBefore.plusDays(i);
			String key = d.format(YMD_BASIC);
			LinkedHashMap<String, Integer> slot = new LinkedHashMap<>();
			slot.put("newAddMember", 0);
			slot.put("vipMember", 0);
			slot.put("svipMember", 0);
			byDate.put(key, slot);
		}

		LambdaQueryWrapper<Statistics> w = new LambdaQueryWrapper<>();
		w.eq(Statistics::getCompanyId, companyId)
				.eq(Statistics::getStatisticType, "member")
				.ge(Statistics::getAddDate, sixDayBeforeInt)
				.le(Statistics::getAddDate, yesterdayInt);
		List<Statistics> rows = statisticsMapper.selectList(w);
		if (rows != null) {
			for (Statistics row : rows) {
				if (row.getAddDate() == null) {
					continue;
				}
				String dk = String.valueOf(row.getAddDate());
				LinkedHashMap<String, Integer> slot = byDate.get(dk);
				if (slot == null) {
					continue;
				}
				String title = row.getStatisticTitle();
				int val = row.getDataValue() == null ? 0 : row.getDataValue();
				if ("newAddMember".equals(title) || "add_user".equals(title)) {
					slot.put("newAddMember", val);
				} else if ("vipMember".equals(title) || "vip_user".equals(title)) {
					slot.put("vipMember", val);
				} else if ("svipMember".equals(title) || "svip_user".equals(title)) {
					slot.put("svipMember", val);
				}
			}
		}

		LinkedHashMap<String, Integer> todaySlot = byDate.computeIfAbsent(todayYmd, k -> {
			LinkedHashMap<String, Integer> s = new LinkedHashMap<>();
			s.put("newAddMember", 0);
			s.put("vipMember", 0);
			s.put("svipMember", 0);
			return s;
		});
		todaySlot.put("newAddMember", scard("Member:" + companyId + ":" + todayYmd));
		todaySlot.put("vipMember", scard("MemberCard:" + companyId + ":vip:" + todayYmd));
		todaySlot.put("svipMember", scard("MemberCard:" + companyId + ":svip:" + todayYmd));

		return reorderAscending(byDate, sixDayBefore, todayYmd);
	}

	private LinkedHashMap<String, LinkedHashMap<String, Integer>> reorderAscending(
			LinkedHashMap<String, LinkedHashMap<String, Integer>> mixed,
			LocalDate sixDayBefore,
			String todayYmd) {
		LinkedHashMap<String, LinkedHashMap<String, Integer>> out = new LinkedHashMap<>();
		for (int i = 0; i < 6; i++) {
			String k = sixDayBefore.plusDays(i).format(YMD_BASIC);
			out.put(k, mixed.getOrDefault(k, emptySlot()));
		}
		out.put(todayYmd, mixed.getOrDefault(todayYmd, emptySlot()));
		return out;
	}

	private LinkedHashMap<String, Integer> emptySlot() {
		LinkedHashMap<String, Integer> s = new LinkedHashMap<>();
		s.put("newAddMember", 0);
		s.put("vipMember", 0);
		s.put("svipMember", 0);
		return s;
	}

	private int scard(String key) {
		Long n = companysRedis.opsForSet().size(key);
		return n == null ? 0 : n > Integer.MAX_VALUE ? Integer.MAX_VALUE : n.intValue();
	}
}
