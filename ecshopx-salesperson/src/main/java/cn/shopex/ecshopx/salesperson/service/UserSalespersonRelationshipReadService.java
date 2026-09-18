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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserSalespersonRelationshipReadService {

	private final WorkWechatRelMapper workWechatRelMapper;
	private final SalespersonGetInfoService salespersonGetInfoService;

	public UserSalespersonRelationshipReadService(WorkWechatRelMapper workWechatRelMapper,
			SalespersonGetInfoService salespersonGetInfoService) {
		this.workWechatRelMapper = workWechatRelMapper;
		this.salespersonGetInfoService = salespersonGetInfoService;
	}

	public Map<String, Object> userSalespersonRelationship(long userId, long companyId, long salespersonId) {
		WorkWechatRel rel = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getSalespersonId, salespersonId)
				.eq(WorkWechatRel::getUserId, userId)
				.last("LIMIT 1"));

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (rel != null) {
			out.put("is_friend", rel.getIsFriend() != null && rel.getIsFriend() ? 1 : 0);
			out.put("is_bind", rel.getIsBind() != null && rel.getIsBind() ? 1 : 0);
		} else {
			out.put("is_friend", 0);
			out.put("is_bind", 0);
		}

		Map<String, Object> detail =
				salespersonGetInfoService.getSalespersonDetailForUserSalespersonRelationship(companyId, salespersonId);
		out.putAll(detail);

		WorkWechatRel boundRow = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE)
				.last("LIMIT 1"));

		if (boundRow != null) {
			out.put("bound", Boolean.TRUE);
			if (boundRow.getSalespersonId() != null) {
				out.put("bound_salesperson", boundRow.getSalespersonId());
			}
		} else {
			out.put("bound", Boolean.FALSE);
		}

		return out;
	}
}
