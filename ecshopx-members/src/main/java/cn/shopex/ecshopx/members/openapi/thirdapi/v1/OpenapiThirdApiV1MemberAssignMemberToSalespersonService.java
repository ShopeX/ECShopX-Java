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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.openapi.OpenapiAssignMemberToSalespersonFailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberAssignMemberToSalespersonService {

	private static final Logger log =
			LoggerFactory.getLogger(OpenapiThirdApiV1MemberAssignMemberToSalespersonService.class);

	private static final DateTimeFormatter ASSIGN_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String MSG_EMPLOYEE_REQUIRED = "请填写导购员工编号";
	private static final String MSG_UNIONID_REQUIRED = "请填写会员unionid";
	private static final String MSG_MEMBER_NOT_FOUND = "会员不存在";

	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MembersMapper membersMapper;

	public OpenapiThirdApiV1MemberAssignMemberToSalespersonService(
			MembersAssociationsMapper membersAssociationsMapper, MembersMapper membersMapper) {
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersMapper = membersMapper;
	}

	public Map<String, Object> executeAssignMemberToSalesperson(
			long companyId, String employeeNumber, String unionid) {
		if (employeeNumber == null || employeeNumber.isEmpty()) {
			throw new OpenapiAssignMemberToSalespersonFailException("E4003", MSG_EMPLOYEE_REQUIRED);
		}
		if (unionid == null || unionid.isEmpty()) {
			throw new OpenapiAssignMemberToSalespersonFailException("E4003", MSG_UNIONID_REQUIRED);
		}

		log.info(
				"分配客户回调通知::params=employee_number={},unionid={}:companyId={}",
				employeeNumber,
				unionid,
				companyId);

		try {
			MembersAssociations assoc =
					membersAssociationsMapper.selectOne(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getUnionid, unionid)
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUserType, "wechat")
									.last("LIMIT 1"));

			if (assoc == null) {
				log.warn("分配客户回调通知::未找到会员 unionid={}:companyId={}", unionid, companyId);
				throw new OpenapiAssignMemberToSalespersonFailException("E4002", MSG_MEMBER_NOT_FOUND);
			}

			Long userId = assoc.getUserId();
			Members member = membersMapper.selectById(userId);

			if (member == null) {
				log.warn("分配客户回调通知::未找到会员记录 user_id={}", userId);
				throw new OpenapiAssignMemberToSalespersonFailException("E4002", MSG_MEMBER_NOT_FOUND);
			}

			if (Objects.equals(member.getFpSalesperson(), employeeNumber)
					&& Boolean.TRUE.equals(member.getHasFp())) {
				log.info(
						"分配客户回调通知::幂等性检查通过，已分配相同导购 user_id={},employee_number={}",
						userId,
						employeeNumber);
				String assignTime = nowAssignTime();
				return successData(employeeNumber, unionid, assignTime);
			}

			member.setFpSalesperson(employeeNumber);
			member.setHasFp(true);
			membersMapper.updateById(member);
			log.info("分配客户回调通知::更新成功 user_id={},employee_number={}", userId, employeeNumber);

			String assignTime = nowAssignTime();
			return successData(employeeNumber, unionid, assignTime);
		} catch (OpenapiAssignMemberToSalespersonFailException e) {
			throw e;
		} catch (Exception e) {
			log.error(
					"分配客户回调通知::异常::{}::employee_number={}::unionid={}",
					e.getMessage(),
					employeeNumber,
					unionid,
					e);
			throw new OpenapiAssignMemberToSalespersonFailException(
					"E5001", "系统错误：" + e.getMessage());
		}
	}

	private static Map<String, Object> successData(
			String employeeNumber, String unionid, String assignTime) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("employee_number", employeeNumber);
		data.put("unionid", unionid);
		data.put("assign_time", assignTime);
		return data;
	}

	private static String nowAssignTime() {
		return LocalDateTime.now().format(ASSIGN_TIME_FORMAT);
	}
}
