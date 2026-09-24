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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableLuckyDrawLogListService {

	private final TurntableLogMapper turntableLogMapper;
	private final MemberAccountService memberAccountService;

	public TurntableLuckyDrawLogListService(
			TurntableLogMapper turntableLogMapper, MemberAccountService memberAccountService) {
		this.turntableLogMapper = turntableLogMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> getLogStatistics(long companyId, long actId, String pageRaw, String pageSizeRaw) {
		int page = parsePage(pageRaw);
		int pageSize = parsePageSizeForLog(pageSizeRaw);
		long total = nullToZero(turntableLogMapper.countAllByActId(actId));
		if (total == 0L) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", total);
			out.put("list", List.of());
			return out;
		}
		LambdaQueryWrapper<TurntableLog> pageWrapper =
				new LambdaQueryWrapper<TurntableLog>()
						.eq(TurntableLog::getActId, actId)
						// 与 countAllByActId / C 端 getLuckyDrawLog 对齐：仅 SUCCESS / GRANT_FAILED
						.in(TurntableLog::getStatus, (Object[]) TurntableDrawStatus.VISIBLE_IN_DRAW_LOG)
						.orderByAsc(TurntableLog::getId);
		Page<TurntableLog> mpPage = new Page<>(page, pageSize, false);
		turntableLogMapper.selectPage(mpPage, pageWrapper);
		List<TurntableLog> records = mpPage.getRecords();

		List<Long> batchUserIds =
				records.stream().map(TurntableLog::getUserId).filter(Objects::nonNull).distinct().toList();
		Map<Long, Map<String, Object>> memberByUserId;
		if (batchUserIds.isEmpty()) {
			memberByUserId = Map.of();
		} else {
			memberByUserId = new HashMap<>();
			List<Map<String, Object>> summaries =
					memberAccountService.listMemberSummariesByUserIds(companyId, batchUserIds);
			for (Map<String, Object> s : summaries) {
				Object uidObj = s.get("user_id");
				if (uidObj == null) {
					continue;
				}
				long uid = uidObj instanceof Number n ? n.longValue() : Long.parseLong(uidObj.toString().trim());
				memberByUserId.merge(uid, s, (a, b) -> a);
			}
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (TurntableLog logRow : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", logRow.getId());
			row.put("company_id", logRow.getCompanyId());
			row.put("user_id", logRow.getUserId());
			row.put("prize_title", logRow.getPrizeTitle());
			row.put("prize_type", logRow.getPrizeType());
			row.put("prize_value", logRow.getPrizeValue());
			row.put("created", logRow.getCreated());
			row.put("act_id", logRow.getActId());
			if (logRow.getUserId() != null) {
				Map<String, Object> mem = memberByUserId.get(logRow.getUserId());
				if (mem != null) {
					Object ucc = mem.get("user_card_code");
					row.put("user_card_code", ucc != null ? ucc.toString() : "");
					Object mob = mem.get("mobile");
					row.put("mobile", mob != null ? mob.toString() : "");
				}
			}
			rows.add(row);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	private static int parsePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return 1;
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSizeForLog(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return 20;
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return 20;
		}
	}

	private static long nullToZero(Long x) {
		return x == null ? 0L : x;
	}
}
