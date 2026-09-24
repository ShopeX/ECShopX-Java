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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableFrontLuckyDrawLogService {

	private final TurntableLogMapper turntableLogMapper;
	private final MessageSource messageSource;

	public TurntableFrontLuckyDrawLogService(
			TurntableLogMapper turntableLogMapper, MessageSource messageSource) {
		this.turntableLogMapper = turntableLogMapper;
		this.messageSource = messageSource;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	public List<Map<String, Object>> getTurnLog(long userId, String idRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		if (idRaw == null) {
			throw new ResourceException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}
		String trimmed = idRaw.trim();
		if (!StringUtils.hasText(trimmed) || "0".equals(trimmed)) {
			throw new ResourceException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}

		long actId;
		try {
			actId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.invalid_activity_id", "活动ID格式错误", locale));
		}
		if (actId <= 0L) {
			throw new BadRequestException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}

		LambdaQueryWrapper<TurntableLog> w = new LambdaQueryWrapper<>();
		w.eq(TurntableLog::getUserId, userId)
				.eq(TurntableLog::getActId, actId);
		applyDrawLogStatusFilter(w);
		w.orderByAsc(TurntableLog::getId);
		List<TurntableLog> rows = turntableLogMapper.selectList(w);
		if (rows.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (TurntableLog logRow : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", logRow.getId());
			row.put("company_id", logRow.getCompanyId());
			row.put("user_id", logRow.getUserId());
			row.put("prize_title", logRow.getPrizeTitle());
			row.put("prize_type", logRow.getPrizeType());
			row.put("prize_value", logRow.getPrizeValue());
			row.put("prize_id", logRow.getPrizeId());
			row.put("status", logRow.getStatus());
			row.put("request_id", logRow.getRequestId());
			row.put("sector_index", logRow.getSectorIndex());
			row.put("created", logRow.getCreated());
			row.put("updated", logRow.getUpdated());
			row.put("act_id", logRow.getActId());
			out.add(row);
		}
		return out;
	}

	/** 与后管日志列表一致的状态过滤，供单测断言。 */
	static void applyDrawLogStatusFilter(LambdaQueryWrapper<TurntableLog> w) {
		// 仅展示 SUCCESS / GRANT_FAILED（不含 PROCESSING、COST_FAILED）
		w.in(TurntableLog::getStatus, (Object[]) TurntableDrawStatus.VISIBLE_IN_DRAW_LOG);
	}
}
