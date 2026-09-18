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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberBatchUpdateGradeService {

	private final MembersMapper membersMapper;

	private final MemberOperateLogMapper memberOperateLogMapper;

	private final ObjectMapper objectMapper;

	public AdminMemberBatchUpdateGradeService(
			MembersMapper membersMapper,
			MemberOperateLogMapper memberOperateLogMapper,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> updateGrade(long companyId, Map<?, ?> jwtClaims, Map<String, Object> merged) {
		Object raw = merged.get("user_ids");
		if (raw == null) {
			throw new ResourceException("未指定用户");
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new ResourceException("未指定用户");
		}
		if (raw instanceof List<?> list && list.isEmpty()) {
			throw new ResourceException("未指定用户");
		}
		if (raw instanceof Collection<?> c && !(raw instanceof List) && c.isEmpty()) {
			throw new ResourceException("未指定用户");
		}

		List<Map<String, Object>> items = parseUserIdsItems(raw);

		Object g = merged.get("grade_id");
		if (g == null) {
			throw new ResourceException("会员等级必填");
		}
		if (g instanceof String gs && !StringUtils.hasText(gs.trim())) {
			throw new ResourceException("会员等级必填");
		}
		long newGradeId;
		try {
			newGradeId = Long.parseLong(String.valueOf(g).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("会员等级必填");
		}

		Object r = merged.get("remarks");
		String remarksForLog = r == null ? "" : String.valueOf(r).trim();

		for (Map<String, Object> item : items) {
			if (!item.containsKey("user_id") || item.get("user_id") == null) {
				throw new ResourceException("会员 id 必填");
			}
			String uidRaw = String.valueOf(item.get("user_id")).trim();
			if (!StringUtils.hasText(uidRaw)) {
				throw new ResourceException("会员 id 必填");
			}
			long userId;
			try {
				userId = Long.parseLong(uidRaw);
			} catch (NumberFormatException e) {
				throw new ResourceException("会员 id 必填");
			}

			Members row =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getUserId, userId)
									.last("LIMIT 1"));
			if (row == null) {
				throw new ResourceException("更新的用户不存在！");
			}
			Long oldGrade = row.getGradeId();
			String oldDataStr = oldGrade == null ? "" : String.valueOf(oldGrade);

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
		}

		return merged;
	}

	private List<Map<String, Object>> parseUserIdsItems(Object raw) {
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>(list.size());
			for (Object elem : list) {
				if (!(elem instanceof Map<?, ?> m)) {
					throw new BadRequestException("用户id格式错误");
				}
				out.add(copyToStringObjectMap(m));
			}
			return out;
		}
		if (raw instanceof String str) {
			JsonNode root;
			try {
				root = objectMapper.readTree(str.trim());
			} catch (JsonProcessingException e) {
				throw new BadRequestException("用户id格式错误");
			}
			if (!root.isArray()) {
				throw new BadRequestException("用户id格式错误");
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (JsonNode node : root) {
				if (!node.isObject()) {
					throw new BadRequestException("用户id格式错误");
				}
				out.add(
						objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {}));
			}
			return out;
		}
		throw new BadRequestException("用户id格式错误");
	}

	private static Map<String, Object> copyToStringObjectMap(Map<?, ?> m) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
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
