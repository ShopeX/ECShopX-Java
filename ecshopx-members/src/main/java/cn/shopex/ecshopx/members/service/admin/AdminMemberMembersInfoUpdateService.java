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

import cn.shopex.ecshopx.common.dispatch.MembersUpdateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberMembersInfoUpdateService {

	private final MembersInfoMapper membersInfoMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;
	private final MembersUpdateMemberSuccessDispatchPublisher membersUpdateMemberSuccessDispatchPublisher;

	public AdminMemberMembersInfoUpdateService(
			MembersInfoMapper membersInfoMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper,
			MembersUpdateMemberSuccessDispatchPublisher membersUpdateMemberSuccessDispatchPublisher) {
		this.membersInfoMapper = membersInfoMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
		this.membersUpdateMemberSuccessDispatchPublisher = membersUpdateMemberSuccessDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateMember(long companyId, long userId, Map<String, Object> postdata) {
		LambdaQueryWrapper<MembersInfo> q =
				new LambdaQueryWrapper<MembersInfo>()
						.eq(MembersInfo::getCompanyId, companyId)
						.eq(MembersInfo::getUserId, userId)
						.last("LIMIT 1");
		MembersInfo info = membersInfoMapper.selectOne(q);
		if (info == null) {
			throw new ResourceException("用户不存在");
		}

		if (postdata.containsKey("other_params")) {
			LinkedHashMap<String, Object> base = decodeOtherParamsMap(info.getOtherParams());
			Map<String, Object> incoming = normalizeIncomingOtherParams(postdata.get("other_params"));
			incoming.forEach(base::put);
			if (base.isEmpty()) {
				info.setOtherParams("[]");
			} else {
				try {
					info.setOtherParams(objectMapper.writeValueAsString(base));
				} catch (JsonProcessingException e) {
					throw new ResourceException("其他参数格式错误");
				}
			}
		} else {
			info.setOtherParams("[]");
		}

		if (postdata.containsKey("username")) {
			Object v = postdata.get("username");
			info.setUsername(v == null ? null : String.valueOf(v).trim());
		}
		if (postdata.containsKey("sex")) {
			Object v = postdata.get("sex");
			if (v == null) {
				info.setSex(null);
			} else if (v instanceof Integer i) {
				info.setSex(i);
			} else if (v instanceof Number n) {
				info.setSex(n.intValue());
			} else {
				try {
					info.setSex(Integer.parseInt(String.valueOf(v).trim()));
				} catch (NumberFormatException e) {
					info.setSex(0);
				}
			}
		}
		if (postdata.containsKey("birthday")) {
			Object v = postdata.get("birthday");
			info.setBirthday(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("address")) {
			Object v = postdata.get("address");
			info.setAddress(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("email")) {
			Object v = postdata.get("email");
			info.setEmail(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("industry")) {
			Object v = postdata.get("industry");
			info.setIndustry(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("income")) {
			Object v = postdata.get("income");
			info.setIncome(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("edu_background")) {
			Object v = postdata.get("edu_background");
			info.setEduBackground(v == null ? null : String.valueOf(v));
		}
		if (postdata.containsKey("habbit")) {
			Object v = postdata.get("habbit");
			if (!(v instanceof java.util.List<?>)) {
				throw new ResourceException("爱好参数格式错误");
			}
			try {
				info.setHabbit(objectMapper.writeValueAsString(v));
			} catch (JsonProcessingException e) {
				throw new ResourceException("爱好参数格式错误");
			}
		}

		info.setUpdated(System.currentTimeMillis() / 1000L);
		int n = membersInfoMapper.updateById(info);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		MembersInfo fresh =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, Object> data = memberAccountService.toMembersInfoApiMap(fresh, false);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("updateMember requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersUpdateMemberSuccessDispatchPublisher.publish(new LinkedHashMap<>(data));
			}
		});
		return data;
	}

	private LinkedHashMap<String, Object> decodeOtherParamsMap(String raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (!StringUtils.hasText(raw)) {
			return out;
		}
		try {
			Object parsed = objectMapper.readValue(raw.trim(), new TypeReference<Object>() {});
			if (parsed instanceof Map<?, ?> pm) {
				for (Map.Entry<?, ?> e : pm.entrySet()) {
					if (e.getKey() != null) {
						out.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
			} else if (parsed instanceof java.util.List<?>) {
				return out;
			}
		} catch (Exception ignored) {
			// treat as empty map
		}
		return out;
	}

	private Map<String, Object> normalizeIncomingOtherParams(Object incomingRaw) {
		if (incomingRaw instanceof Map<?, ?> pm) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : pm.entrySet()) {
				if (e.getKey() != null) {
					m.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return m;
		}
		if (incomingRaw instanceof String str) {
			if (!StringUtils.hasText(str)) {
				return new LinkedHashMap<>();
			}
			try {
				Object p = objectMapper.readValue(str.trim(), new TypeReference<Object>() {});
				if (!(p instanceof Map)) {
					throw new ResourceException("其他参数格式错误");
				}
				return normalizeIncomingOtherParams(p);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				throw new ResourceException("其他参数格式错误");
			}
		}
		throw new ResourceException("其他参数格式错误");
	}
}
