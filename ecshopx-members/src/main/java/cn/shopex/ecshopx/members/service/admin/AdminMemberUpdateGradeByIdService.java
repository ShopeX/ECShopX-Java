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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberUpdateGradeByIdService {

	private final MembersMapper membersMapper;

	private final MemberOperateLogMapper memberOperateLogMapper;

	public AdminMemberUpdateGradeByIdService(
			MembersMapper membersMapper, MemberOperateLogMapper memberOperateLogMapper) {
		this.membersMapper = membersMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
	}

	public LinkedHashMap<String, Object> updateGradeById(
			long companyId, Map<?, ?> jwtClaims, Map<String, Object> merged) {
		long userId = parseUserIdFilter(merged.get("user_id"));
		long newGradeId = parseGradeId(merged.get("grade_id"));

		Object r = merged.get("remarks");
		String remarksForLog = r == null ? "" : String.valueOf(r);
		Object o = merged.get("old_grade_id");
		String oldDataStr = o == null ? "" : String.valueOf(o);

		Members row =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("更新的用户不存在！");
		}

		LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
		uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
		uw.set(Members::getGradeId, newGradeId);
		uw.set(Members::getUpdated, System.currentTimeMillis() / 1000L);
		int affected = membersMapper.update(null, uw);

		if (affected > 0) {
			long nowSec = System.currentTimeMillis() / 1000L;
			MemberOperateLog log = new MemberOperateLog();
			log.setCompanyId(companyId);
			log.setUserId(userId);
			log.setOperateType("grade_id");
			log.setRemarks(remarksForLog);
			log.setOldData(oldDataStr);
			log.setNewData(String.valueOf(newGradeId));
			log.setOperater(buildOperaterDescription(jwtClaims));
			log.setCreated(nowSec);
			log.setUpdated(nowSec);
			memberOperateLogMapper.insert(log);
		} else {
			throw new ResourceException("更新失败，user_id=" + userId);
		}

		Members refreshed =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (refreshed == null) {
			throw new ResourceException("更新的用户不存在！");
		}

		LinkedHashMap<String, Object> data = MemberAccountService.mapMembersTable(refreshed);
		data.put("mobile", LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(refreshed.getMobile()));
		if (refreshed.getSourceId() == null) {
			data.put("source_id", null);
		} else {
			data.put("source_id", refreshed.getSourceId());
		}
		if (refreshed.getMonitorId() == null) {
			data.put("monitor_id", null);
		} else {
			data.put("monitor_id", refreshed.getMonitorId());
		}
		if (refreshed.getLatestSourceId() == null) {
			data.put("latest_source_id", null);
		} else {
			data.put("latest_source_id", refreshed.getLatestSourceId());
		}
		if (refreshed.getLatestMonitorId() == null) {
			data.put("latest_monitor_id", null);
		} else {
			data.put("latest_monitor_id", refreshed.getLatestMonitorId());
		}
		return data;
	}

	private static long parseUserIdFilter(Object rawUid) {
		if (rawUid == null) {
			throw new ResourceException("更新的用户不存在！");
		}
		if (rawUid instanceof Number num) {
			double d = num.doubleValue();
			if (d <= 0.0 || d != Math.rint(d) || d > (double) Long.MAX_VALUE) {
				throw new ResourceException("更新的用户不存在！");
			}
			return num.longValue();
		}
		String u = String.valueOf(rawUid).trim();
		if (!StringUtils.hasText(u)) {
			throw new ResourceException("更新的用户不存在！");
		}
		try {
			long parsed = Long.parseLong(u);
			if (parsed <= 0L) {
				throw new ResourceException("更新的用户不存在！");
			}
			return parsed;
		} catch (NumberFormatException e) {
			throw new ResourceException("更新的用户不存在！");
		}
	}

	private static long parseGradeId(Object g) {
		if (g == null) {
			throw new ResourceException("会员等级必填");
		}
		if (g instanceof String gs && !StringUtils.hasText(gs.trim())) {
			throw new ResourceException("会员等级必填");
		}
		try {
			return Long.parseLong(String.valueOf(g).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("会员等级必填");
		}
	}

	private static String buildOperaterDescription(Map<?, ?> jwt) {
		String opType = String.valueOf(jwt.get("operator_type")).trim();
		if ("staff".equalsIgnoreCase(opType)) {
			return "员工-" + nullSafe(jwt.get("username")) + "-" + nullSafe(jwt.get("mobile"));
		}
		return nullSafe(jwt.get("username"));
	}

	private static String nullSafe(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
