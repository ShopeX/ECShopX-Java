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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.workwechat.domain.WorkWechatMessage;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatMessageService {

	private final WorkWechatMessageMapper workWechatMessageMapper;

	public WorkWechatMessageService(WorkWechatMessageMapper mapper) {
		this.workWechatMessageMapper = mapper;
	}

	public Map<String, Object> getList(
			long companyId,
			long operatorId,
			int msgType,
			long distributorId,
			@Nullable Long cursorIdLt,
			int page,
			int pageSize) {
		LambdaQueryWrapper<WorkWechatMessage> countWrap = new LambdaQueryWrapper<>();
		applyListFilter(countWrap, companyId, operatorId, msgType, distributorId, cursorIdLt);
		long total = workWechatMessageMapper.selectCount(countWrap);

		List<Map<String, Object>> listRows;
		if (total == 0L) {
			listRows = Collections.emptyList();
		} else {
			LambdaQueryWrapper<WorkWechatMessage> listWrap = new LambdaQueryWrapper<>();
			applyListFilter(listWrap, companyId, operatorId, msgType, distributorId, cursorIdLt);
			listWrap.orderByDesc(WorkWechatMessage::getId);
			if (pageSize > 0) {
				Page<WorkWechatMessage> p = new Page<>(page, pageSize);
				p.setSearchCount(false);
				workWechatMessageMapper.selectPage(p, listWrap);
				List<WorkWechatMessage> records = p.getRecords();
				listRows = new ArrayList<>(records.size());
				for (WorkWechatMessage e : records) {
					listRows.add(toListRow(e));
				}
			} else {
				List<WorkWechatMessage> records = workWechatMessageMapper.selectList(listWrap);
				listRows = new ArrayList<>(records.size());
				for (WorkWechatMessage e : records) {
					listRows.add(toListRow(e));
				}
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", listRows);
		return out;
	}

	public Map<String, Object> getNewInfo(long companyId, long operatorId, long distributorId) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (int k = 1; k <= 3; k++) {
			LambdaQueryWrapper<WorkWechatMessage> countW = new LambdaQueryWrapper<>();
			applyBaseFilter(countW, companyId, operatorId, k, distributorId);
			countW.eq(WorkWechatMessage::getIsRead, 0);
			long numK = workWechatMessageMapper.selectCount(countW);
			out.put("num_type_" + k, numK);

			LambdaQueryWrapper<WorkWechatMessage> latestW = new LambdaQueryWrapper<>();
			applyBaseFilter(latestW, companyId, operatorId, k, distributorId);
			latestW.orderByDesc(WorkWechatMessage::getAddTime).orderByDesc(WorkWechatMessage::getId);
			latestW.last("LIMIT 1");
			List<WorkWechatMessage> rows = workWechatMessageMapper.selectList(latestW);
			WorkWechatMessage row = rows.isEmpty() ? null : rows.get(0);
			String dateStr = formatAddTimeForDisplay(row == null ? null : row.getAddTime());
			out.put("date_type_" + k, dateStr);
		}

		long n1 = (Long) out.get("num_type_1");
		long n2 = (Long) out.get("num_type_2");
		long n3 = (Long) out.get("num_type_3");
		String d1 = (String) out.get("date_type_1");
		String d2 = (String) out.get("date_type_2");
		String d3 = (String) out.get("date_type_3");
		boolean numsEmpty = n1 == 0L && n2 == 0L && n3 == 0L;
		boolean datesEmpty = isBlankDate(d1) && isBlankDate(d2) && isBlankDate(d3);
		int isEmpty = numsEmpty && datesEmpty ? 1 : 0;
		out.put("is_empty", isEmpty);
		return out;
	}

	private static boolean isBlankDate(String date) {
		return date == null || date.isBlank();
	}

	private static void applyBaseFilter(
			LambdaQueryWrapper<WorkWechatMessage> w,
			long companyId,
			long operatorId,
			int msgType,
			long distributorId) {
		w.eq(WorkWechatMessage::getDistributorId, distributorId)
				.eq(WorkWechatMessage::getCompanyId, companyId)
				.eq(WorkWechatMessage::getOperatorId, operatorId)
				.eq(WorkWechatMessage::getMsgType, msgType);
	}

	private static String formatAddTimeForDisplay(Integer addTimeUnix) {
		if (addTimeUnix == null || addTimeUnix == 0) {
			return "";
		}
		ZonedDateTime zdt = Instant.ofEpochSecond(addTimeUnix.longValue()).atZone(ZoneId.of("Asia/Shanghai"));
		return zdt.format(DateTimeFormatter.ofPattern("MM月dd日", Locale.CHINA));
	}

	private static void applyListFilter(
			LambdaQueryWrapper<WorkWechatMessage> w,
			long companyId,
			long operatorId,
			int msgType,
			long distributorId,
			@Nullable Long cursorIdLt) {
		w.eq(WorkWechatMessage::getDistributorId, distributorId)
				.eq(WorkWechatMessage::getCompanyId, companyId)
				.eq(WorkWechatMessage::getOperatorId, operatorId)
				.eq(WorkWechatMessage::getMsgType, msgType);
		if (cursorIdLt != null) {
			w.lt(WorkWechatMessage::getId, cursorIdLt);
		}
	}

	private static Map<String, Object> toListRow(WorkWechatMessage e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("msg_type", e.getMsgType());
		m.put("content", e.getContent());
		m.put("add_time", e.getAddTime());
		m.put("is_read", e.getIsRead());
		m.put("up_time", e.getUpTime());
		return m;
	}

	public void updateMsg(
			long companyId,
			long operatorId,
			int msgType,
			long distributorId,
			@Nullable String contentLikeInnerFragment,
			int upTimeUnixSeconds) {
		LambdaUpdateWrapper<WorkWechatMessage> wrapper = new LambdaUpdateWrapper<>();
		wrapper.eq(WorkWechatMessage::getMsgType, msgType)
				.eq(WorkWechatMessage::getCompanyId, companyId)
				.eq(WorkWechatMessage::getOperatorId, operatorId)
				.eq(WorkWechatMessage::getDistributorId, distributorId)
				.eq(WorkWechatMessage::getIsRead, 0);
		if (contentLikeInnerFragment != null && !contentLikeInnerFragment.isEmpty()) {
			wrapper.like(WorkWechatMessage::getContent, contentLikeInnerFragment);
		}
		WorkWechatMessage patch = new WorkWechatMessage();
		patch.setIsRead(1);
		patch.setUpTime(upTimeUnixSeconds);
		this.workWechatMessageMapper.update(patch, wrapper);
	}
}
