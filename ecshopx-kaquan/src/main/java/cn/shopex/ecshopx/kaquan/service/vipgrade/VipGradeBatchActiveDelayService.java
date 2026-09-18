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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.kaquan.port.BatchReceiveMemberCardDispatchPublisher;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VipGradeBatchActiveDelayService {

	private record UserBatchRow(long userId, String mobilePlain) {}

	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final VipGradeOrderReceiveService vipGradeOrderReceiveService;
	private final BatchReceiveMemberCardDispatchPublisher batchReceiveMemberCardDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public VipGradeBatchActiveDelayService(
			VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			VipGradeOrderReceiveService vipGradeOrderReceiveService,
			BatchReceiveMemberCardDispatchPublisher batchReceiveMemberCardDispatchPublisher,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.vipGradeOrderReceiveService = vipGradeOrderReceiveService;
		this.batchReceiveMemberCardDispatchPublisher = batchReceiveMemberCardDispatchPublisher;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void processBatchActiveDelay(long companyId, long vipGradeId, int addDay, String filter, String usersJson) {
		VipGrade grade = vipGradeMapper.selectOne(new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId)
				.eq(VipGrade::getVipGradeId, vipGradeId));
		if (grade == null) {
			throw new ResourceException("无效会员卡等级");
		}
		if (addDay <= 0) {
			throw new BadRequestException("延期天数无效");
		}

		List<UserBatchRow> syncUsers = new ArrayList<>();

		if ("expired".equals(filter)) {
			String vipType = grade.getLvType();
			if (vipType == null) {
				vipType = "vip";
			}
			long expiredCount = countExpiredVipGrade(companyId, vipType);
			if (expiredCount <= 50) {
				syncUsers.addAll(listExpiredVipGradeUsers(companyId, vipType, 1, 100));
			} else {
				batchReceiveMemberCardDispatchPublisher.publish(companyId, vipGradeId, addDay, vipType);
			}
		} else if ("users".equals(filter)) {
			if (usersJson != null && !usersJson.isBlank()) {
				List<Map<String, Object>> rows = null;
				try {
					rows = objectMapper.readValue(usersJson, new TypeReference<List<Map<String, Object>>>() {});
				} catch (JsonProcessingException e) {
					rows = null;
				}
				if (rows != null) {
					for (Map<String, Object> row : rows) {
						long userId = parseUserIdFromRow(row);
						String mobile = "";
						Object m = row.get("mobile");
						if (m != null) {
							mobile = m.toString();
						}
						syncUsers.add(new UserBatchRow(userId, mobile));
					}
				}
			}
		}

		if (syncUsers.isEmpty()) {
			return;
		}

		try {
			transactionTemplate.execute(status -> {
				for (UserBatchRow row : syncUsers) {
					vipGradeOrderReceiveService.receiveAdminCustomDelay(
							companyId, row.userId(), row.mobilePlain(), vipGradeId, addDay);
				}
				return null;
			});
		} catch (Exception e) {
			if (e instanceof ResourceException re) {
				throw re;
			}
			throw new ResourceException(e.getMessage());
		}
	}

	private static long parseUserIdFromRow(Map<String, Object> row) {
		Object raw = row.get("user_id");
		if (raw == null) {
			throw new BadRequestException("users 格式错误");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("users 格式错误");
			}
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("users 格式错误");
		}
	}

	private long countExpiredVipGrade(long companyId, String vipType) {
		long nowSec = Instant.now().getEpochSecond();
		return vipGradeRelUserMapper.selectCount(new LambdaQueryWrapper<VipGradeRelUser>()
				.eq(VipGradeRelUser::getCompanyId, (int) companyId)
				.eq(VipGradeRelUser::getVipType, vipType)
				.apply("CAST(end_date AS UNSIGNED) < {0}", nowSec));
	}

	private List<UserBatchRow> listExpiredVipGradeUsers(long companyId, String vipType, int page, int pageSize) {
		long nowSec = Instant.now().getEpochSecond();
		LambdaQueryWrapper<VipGradeRelUser> wrapper = new LambdaQueryWrapper<VipGradeRelUser>()
				.eq(VipGradeRelUser::getCompanyId, (int) companyId)
				.eq(VipGradeRelUser::getVipType, vipType)
				.apply("CAST(end_date AS UNSIGNED) < {0}", nowSec)
				.orderByDesc(VipGradeRelUser::getId);
		Page<VipGradeRelUser> mpPage = new Page<>(page, pageSize);
		Page<VipGradeRelUser> result = vipGradeRelUserMapper.selectPage(mpPage, wrapper);
		List<VipGradeRelUser> relRows = result.getRecords();
		if (relRows == null || relRows.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> userIds = relRows.stream().map(VipGradeRelUser::getUserId).distinct().toList();
		if (userIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Members> members = membersMapper.selectList(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.in(Members::getUserId, userIds));
		Map<Long, Members> byUserId = members.stream().collect(Collectors.toMap(Members::getUserId, m -> m, (a, b) -> a));
		List<UserBatchRow> out = new ArrayList<>(relRows.size());
		for (VipGradeRelUser rel : relRows) {
			Long uid = rel.getUserId();
			Members member = byUserId.get(uid);
			String mobilePlain = "";
			if (member != null) {
				String decrypted = sensitiveFieldEncryptor.decrypt(member.getMobile());
				if (decrypted != null && !decrypted.isBlank()) {
					mobilePlain = decrypted;
				} else if (member.getRegionMobile() != null && !member.getRegionMobile().isBlank()) {
					mobilePlain = member.getRegionMobile();
				}
			}
			out.add(new UserBatchRow(uid, mobilePlain));
		}
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public void runExpiredJobInOneTransaction(long companyId, long vipGradeId, int addDay, String lvType) {
		long total = countExpiredVipGrade(companyId, lvType);
		int limit = 50;
		int pages = (int) Math.ceil(total / (double) limit);
		for (int i = 1; i <= pages; i++) {
			List<UserBatchRow> users = listExpiredVipGradeUsers(companyId, lvType, i, limit);
			for (UserBatchRow row : users) {
				vipGradeOrderReceiveService.receiveAdminCustomDelay(
						companyId, row.userId(), row.mobilePlain(), vipGradeId, addDay);
			}
		}
	}
}
