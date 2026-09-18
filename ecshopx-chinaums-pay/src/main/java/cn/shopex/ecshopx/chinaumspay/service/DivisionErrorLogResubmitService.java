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

package cn.shopex.ecshopx.chinaumspay.service;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DivisionErrorLogResubmitService {

	public static final int IS_RESUBMIT_NOT = 0;
	public static final int IS_RESUBMIT_SUCC = 1;
	public static final int IS_RESUBMIT_WAITING = 2;

	private final ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper;

	public DivisionErrorLogResubmitService(ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper) {
		this.chinaumspayDivisionErrorLogMapper = chinaumspayDivisionErrorLogMapper;
	}

	public Map<String, Object> resubmit(long companyId, String idPath) {
		long recordId = parseRecordId(idPath);
		LambdaQueryWrapper<ChinaumspayDivisionErrorLog> qw = new LambdaQueryWrapper<>();
		qw.eq(ChinaumspayDivisionErrorLog::getCompanyId, companyId)
				.eq(ChinaumspayDivisionErrorLog::getId, recordId);
		ChinaumspayDivisionErrorLog entity = chinaumspayDivisionErrorLogMapper.selectOne(qw);
		if (entity == null) {
			throw new ResourceException("查询数据失败，请稍后重试");
		}
		Integer v = entity.getIsResubmit();
		if (v != null && v != IS_RESUBMIT_NOT) {
			throw new ResourceException("不需要重新提交，请稍后重试");
		}
		ChinaumspayDivisionErrorLog row = new ChinaumspayDivisionErrorLog();
		row.setIsResubmit(IS_RESUBMIT_WAITING);
		LambdaUpdateWrapper<ChinaumspayDivisionErrorLog> uw = new LambdaUpdateWrapper<>();
		uw.eq(ChinaumspayDivisionErrorLog::getCompanyId, companyId)
				.eq(ChinaumspayDivisionErrorLog::getId, recordId);
		int rows = chinaumspayDivisionErrorLogMapper.update(row, uw);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", rows);
		return body;
	}

	private static long parseRecordId(String idPath) {
		if (idPath == null) {
			return -1L;
		}
		String t = idPath.trim();
		if (t.isEmpty()) {
			return -1L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return -1L;
		}
	}
}
