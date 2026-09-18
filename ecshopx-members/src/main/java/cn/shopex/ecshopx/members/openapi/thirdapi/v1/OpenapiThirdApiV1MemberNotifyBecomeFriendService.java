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

import cn.shopex.ecshopx.common.openapi.OpenapiNotifyBecomeFriendFailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1MemberNotifyBecomeFriendService {

	private static final Logger log =
			LoggerFactory.getLogger(OpenapiThirdApiV1MemberNotifyBecomeFriendService.class);

	private static final String MSG_UNIONID_REQUIRED = "请填写会员unionid";
	private static final String MSG_SALESPERSON_REQUIRED = "请填写导购编号";
	private static final String MSG_IS_BECOME_FRIEND_REQUIRED = "is_become_friend必须为1";
	private static final String MSG_MEMBER_NOT_FOUND = "会员不存在";
	private static final String MSG_NOT_ASSIGNED = "该会员未分配给该导购";

	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MembersMapper membersMapper;

	public OpenapiThirdApiV1MemberNotifyBecomeFriendService(
			MembersAssociationsMapper membersAssociationsMapper, MembersMapper membersMapper) {
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersMapper = membersMapper;
	}

	public void executeNotifyBecomeFriend(
			long companyId, String unionid, String salespersonCode, String isBecomeFriendRaw) {
		if (unionid == null || unionid.isEmpty()) {
			throw new OpenapiNotifyBecomeFriendFailException("E0001", MSG_UNIONID_REQUIRED);
		}
		if (salespersonCode == null || salespersonCode.isEmpty()) {
			throw new OpenapiNotifyBecomeFriendFailException("E0001", MSG_SALESPERSON_REQUIRED);
		}
		validateIsBecomeFriend(isBecomeFriendRaw);

		log.info(
				"导购通知加好友::params=unionid={},salesperson_code={},is_become_friend={}:companyId={}",
				unionid,
				salespersonCode,
				isBecomeFriendRaw,
				companyId);

		log.info(
				"[MemberService] 导购通知加好友：开始处理 company_id={}, unionid={}, salesperson_code={}",
				companyId,
				unionid,
				salespersonCode);

		try {
			MembersAssociations assoc =
					membersAssociationsMapper.selectOne(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getUnionid, unionid)
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUserType, "wechat")
									.last("LIMIT 1"));

			if (assoc == null) {
				log.warn(
						"[MemberService] 导购通知加好友：未找到会员 unionid={}:companyId={}",
						unionid,
						companyId);
				throw new OpenapiNotifyBecomeFriendFailException("E4002", MSG_MEMBER_NOT_FOUND);
			}

			Long userId = assoc.getUserId();
			Members member = membersMapper.selectById(userId);

			if (member == null) {
				log.warn("[MemberService] 导购通知加好友：未找到会员记录 user_id={}", userId);
				throw new OpenapiNotifyBecomeFriendFailException("E4002", MSG_MEMBER_NOT_FOUND);
			}

			if (!Boolean.TRUE.equals(member.getHasFp())
					|| !Objects.equals(String.valueOf(member.getFpSalesperson()), salespersonCode)) {
				log.warn(
						"[MemberService] 导购通知加好友：会员未分配给该导购 user_id={}, salesperson_code={}, has_fp={}, fp_salesperson={}",
						userId,
						salespersonCode,
						member.getHasFp(),
						member.getFpSalesperson());
				throw new OpenapiNotifyBecomeFriendFailException("E4004", MSG_NOT_ASSIGNED);
			}

			if (Boolean.TRUE.equals(member.getIsBecomeFriend())) {
				log.info(
						"[MemberService] 导购通知加好友：幂等性检查通过，已是好友状态 user_id={}, salesperson_code={}",
						userId,
						salespersonCode);
				return;
			}

			member.setIsBecomeFriend(true);
			membersMapper.updateById(member);
			log.info(
					"[MemberService] 导购通知加好友：更新成功 user_id={}, salesperson_code={}",
					userId,
					salespersonCode);
		} catch (OpenapiNotifyBecomeFriendFailException e) {
			throw e;
		} catch (Exception e) {
			log.error(
					"导购通知加好友::异常::{}::unionid={}::salesperson_code={}",
					e.getMessage(),
					unionid,
					salespersonCode,
					e);
			throw new OpenapiNotifyBecomeFriendFailException(
					"E5001", "系统错误：" + e.getMessage());
		}
	}

	private static void validateIsBecomeFriend(String raw) {
		if (!"1".equals(raw)) {
			throw new OpenapiNotifyBecomeFriendFailException("E0001", MSG_IS_BECOME_FRIEND_REQUIRED);
		}
	}
}
