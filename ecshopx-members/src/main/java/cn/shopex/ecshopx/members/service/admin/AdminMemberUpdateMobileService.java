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

import cn.shopex.ecshopx.common.exception.BadRequestException;
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
public class AdminMemberUpdateMobileService {

	private final MembersMapper membersMapper;

	private final MemberOperateLogMapper memberOperateLogMapper;

	public AdminMemberUpdateMobileService(
			MembersMapper membersMapper, MemberOperateLogMapper memberOperateLogMapper) {
		this.membersMapper = membersMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
	}

	public LinkedHashMap<String, Object> updateMobileById(
			long companyId, Map<?, ?> jwtClaims, Map<String, Object> merged) {
		String newPlain = normalizeNewMobilePlain(merged.get("newMobile"));
		if (newPlain == null) {
			throw new BadRequestException("请填写正确的手机号");
		}

		String oldPlain = null;
		if (merged.get("oldMobile") != null) {
			String s = String.valueOf(merged.get("oldMobile")).trim();
			if (StringUtils.hasText(s)) {
				oldPlain = s;
			}
		}

		long userIdFilter = parseUserIdFilter(merged.get("user_id"));

		LambdaQueryWrapper<Members> q = new LambdaQueryWrapper<>();
		q.eq(Members::getCompanyId, companyId);
		q.eq(Members::getUserId, userIdFilter);
		if (oldPlain != null) {
			q.eq(Members::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(oldPlain));
		}
		q.last("LIMIT 1");
		Members target = membersMapper.selectOne(q);
		if (target == null) {
			throw new ResourceException("更新的用户不存在！");
		}

		String newStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(newPlain);
		LambdaQueryWrapper<Members> dup = new LambdaQueryWrapper<>();
		dup.eq(Members::getCompanyId, companyId).eq(Members::getMobile, newStored).last("LIMIT 1");
		Members occupant = membersMapper.selectOne(dup);
		if (occupant != null) {
			throw new ResourceException("用户手机号已经存在");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
		uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, target.getUserId());
		uw.set(Members::getMobile, newStored);
		uw.set(Members::getUpdated, nowSec);
		int affected = membersMapper.update(null, uw);
		if (affected == 0) {
			throw new ResourceException("user_id=" + target.getUserId() + "的会员不存在");
		}

		Members row =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, target.getUserId())
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("更新的用户不存在！");
		}
		LinkedHashMap<String, Object> data = MemberAccountService.mapMembersTable(row);
		data.put("mobile", newPlain);
		if (row.getSourceId() == null) {
			data.put("source_id", null);
		} else {
			data.put("source_id", row.getSourceId());
		}
		if (row.getMonitorId() == null) {
			data.put("monitor_id", null);
		} else {
			data.put("monitor_id", row.getMonitorId());
		}
		if (row.getLatestSourceId() == null) {
			data.put("latest_source_id", null);
		} else {
			data.put("latest_source_id", row.getLatestSourceId());
		}
		if (row.getLatestMonitorId() == null) {
			data.put("latest_monitor_id", null);
		} else {
			data.put("latest_monitor_id", row.getLatestMonitorId());
		}

		MemberOperateLog log = new MemberOperateLog();
		log.setCompanyId(companyId);
		log.setUserId(target.getUserId());
		log.setOperateType("mobile");
		log.setRemarks("");
		log.setOldData(oldPlain == null ? "" : oldPlain);
		log.setNewData(newPlain);
		log.setOperater(buildOperaterDescription(jwtClaims));
		log.setCreated(nowSec);
		log.setUpdated(nowSec);
		memberOperateLogMapper.insert(log);

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

	private static String normalizeNewMobilePlain(Object rawNew) {
		if (rawNew == null) {
			return null;
		}
		if (rawNew instanceof Boolean b) {
			if (!b) {
				return null;
			}
			String t = String.valueOf(rawNew).trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		if (rawNew instanceof Number num) {
			if (num.doubleValue() == 0.0) {
				return null;
			}
			boolean useLongString =
					rawNew instanceof Long
							|| rawNew instanceof Integer
							|| rawNew instanceof Short
							|| rawNew instanceof Byte;
			if (!useLongString) {
				double d = num.doubleValue();
				if (d == Math.rint(d) && Math.abs(d) <= (double) Long.MAX_VALUE) {
					useLongString = true;
				}
			}
			if (useLongString) {
				String plain = String.valueOf(num.longValue());
				if ("0".equals(plain)) {
					return null;
				}
				return plain;
			}
			String t = String.valueOf(rawNew).trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		if (rawNew instanceof String str) {
			String t = str.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
			return t;
		}
		String t = String.valueOf(rawNew).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		return t;
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
