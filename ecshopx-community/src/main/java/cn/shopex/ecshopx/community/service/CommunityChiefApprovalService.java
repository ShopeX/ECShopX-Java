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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChiefApplyInfo;
import cn.shopex.ecshopx.community.mapper.CommunityChiefApplyInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefApprovalService {

	private static final Logger log = LoggerFactory.getLogger(CommunityChiefApprovalService.class);

	private final CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper;
	private final MemberAccountService memberAccountService;
	private final CommunityChiefService communityChiefService;

	public CommunityChiefApprovalService(
			CommunityChiefApplyInfoMapper communityChiefApplyInfoMapper,
			MemberAccountService memberAccountService,
			CommunityChiefService communityChiefService) {
		this.communityChiefApplyInfoMapper = communityChiefApplyInfoMapper;
		this.memberAccountService = memberAccountService;
		this.communityChiefService = communityChiefService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void approve(long companyId, int distributorId, long applyId, Map<String, ?> mergedInput) {
		int approveStatus = resolveApproveStatus(mergedInput);
		String refuseReason = null;
		if (approveStatus == 2) {
			refuseReason = requireRefuseReason(mergedInput);
		}

		try {
			CommunityChiefApplyInfo applyInfo = communityChiefApplyInfoMapper.selectOne(
					new LambdaQueryWrapper<CommunityChiefApplyInfo>()
							.eq(CommunityChiefApplyInfo::getCompanyId, companyId)
							.eq(CommunityChiefApplyInfo::getDistributorId, distributorId)
							.eq(CommunityChiefApplyInfo::getApplyId, applyId)
							.eq(CommunityChiefApplyInfo::getApproveStatus, 0)
							.last("LIMIT 1"));
			if (applyInfo == null) {
				throw new ResourceException("找不到申请信息");
			}

			int nowTs = (int) (System.currentTimeMillis() / 1000L);
			LambdaUpdateWrapper<CommunityChiefApplyInfo> uw = new LambdaUpdateWrapper<>();
			uw.eq(CommunityChiefApplyInfo::getCompanyId, companyId)
					.eq(CommunityChiefApplyInfo::getDistributorId, distributorId)
					.eq(CommunityChiefApplyInfo::getApplyId, applyId)
					.eq(CommunityChiefApplyInfo::getApproveStatus, 0)
					.set(CommunityChiefApplyInfo::getApproveStatus, approveStatus)
					.set(CommunityChiefApplyInfo::getUpdatedAt, nowTs);
			if (approveStatus == 2) {
				uw.set(CommunityChiefApplyInfo::getRefuseReason, refuseReason);
			}
			int updated = communityChiefApplyInfoMapper.update(null, uw);
			if (updated == 0) {
				throw new ResourceException("找不到申请信息");
			}

			if (approveStatus == 1) {
				createChiefFromApprovedApply(applyInfo);
			}
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("community chief approve failed, applyId={}", applyId, e);
			throw new ResourceException(e.getMessage());
		}
	}

	private static int resolveApproveStatus(Map<String, ?> mergedInput) {
		Object raw = mergedInput.get("approve_status");
		if (isMissingOrBlankApproveStatus(raw)) {
			throw new BadRequestException("审批状态必填");
		}
		int v = parseIntLooseForApprove(raw);
		if (v != 1 && v != 2) {
			throw new BadRequestException("审批状态必填");
		}
		return v;
	}

	private static boolean isMissingOrBlankApproveStatus(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	private static int parseIntLooseForApprove(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return -1;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String requireRefuseReason(Map<String, ?> mergedInput) {
		Object rr = mergedInput.get("refuse_reason");
		if (rr == null || !StringUtils.hasText(String.valueOf(rr).trim())) {
			throw new BadRequestException("拒绝原因必填");
		}
		return String.valueOf(rr).trim();
	}

	private void createChiefFromApprovedApply(CommunityChiefApplyInfo apply) {
		Long companyId = apply.getCompanyId();
		if (companyId == null || companyId == 0L) {
			throw new ResourceException("参数错误");
		}
		Integer dist = apply.getDistributorId();
		List<Integer> distributorIds = List.of(dist != null ? dist : 0);

		Long userId = apply.getUserId();
		if (userId == null || userId == 0L) {
			throw new ResourceException("无效的用户");
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		if (memberInfo == null || memberInfo.isEmpty()) {
			throw new ResourceException("无效的会员");
		}

		long chiefCompanyId = CommunityChiefService.extractCompanyId(memberInfo, companyId);
		String chiefNameParam = apply.getChiefName();
		String chiefMobileParam = apply.getChiefMobile();
		String chiefName =
				StringUtils.hasText(chiefNameParam)
						? chiefNameParam.trim()
						: CommunityChiefService.stringVal(memberInfo.get("username"));
		String chiefAvatar = CommunityChiefService.stringVal(memberInfo.get("avatar"));
		String chiefMobile =
				StringUtils.hasText(chiefMobileParam)
						? chiefMobileParam.trim()
						: CommunityChiefService.stringVal(memberInfo.get("mobile"));
		Long memberUserId = CommunityChiefService.toLongUserId(memberInfo.get("user_id"));
		if (memberUserId == null || memberUserId == 0L) {
			throw new ResourceException("无效的会员");
		}

		ChiefProfileInput profile = new ChiefProfileInput(chiefName, chiefAvatar, chiefMobile, "", "");
		communityChiefService.upsertChiefDistributorsAndShopRels(companyId, chiefCompanyId, memberUserId, distributorIds, profile);
	}
}
