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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.RightsTransferLogs;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsTransferLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsAdminTransferRightsService {

	public sealed interface TransferRightsResult permits Success, Embedded412 {}

	public record Success() implements TransferRightsResult {}

	public record Embedded412(String message) implements TransferRightsResult {}

	private final RightsMapper rightsMapper;
	private final RightsTransferLogsMapper rightsTransferLogsMapper;
	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RightsAdminTransferRightsService(
			RightsMapper rightsMapper,
			RightsTransferLogsMapper rightsTransferLogsMapper,
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.rightsMapper = rightsMapper;
		this.rightsTransferLogsMapper = rightsTransferLogsMapper;
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public TransferRightsResult transferRights(
			long companyId,
			long rightsId,
			String mobileFromRequest,
			String transferMobile,
			String remark) {
		Rights rightsDetail = rightsMapper.selectById(rightsId);
		if (rightsDetail == null) {
			throw new ResourceException("rights_id=" + rightsId + "的权益不存在");
		}
		if (!Objects.equals(rightsDetail.getCompanyId(), companyId)) {
			throw new ResourceException("权益不存在");
		}

		Long userId = rightsDetail.getUserId();
		boolean userIdTruthy = userId != null && userId != 0L;
		if (!userIdTruthy) {
			return new Embedded412("请填写手机号");
		}

		LambdaQueryWrapper<Rights> w =
				Wrappers.<Rights>lambdaQuery()
						.eq(Rights::getCompanyId, companyId)
						.eq(Rights::getUserId, userId)
						.orderByAsc(Rights::getEndTime);
		Page<Rights> mpPage = new Page<>(1, 100);
		Page<Rights> pageResult = rightsMapper.selectPage(mpPage, w);

		String mobile = mobileFromRequest;
		if (pageResult.getRecords() != null && !pageResult.getRecords().isEmpty()) {
			Rights first = pageResult.getRecords().get(0);
			String enc = first.getMobile() == null ? "" : first.getMobile();
			mobile = sensitiveFieldEncryptor.decrypt(enc);
		}

		String tm = transferMobile == null ? "" : transferMobile.trim();
		if (!StringUtils.hasText(tm) || "0".equals(tm)) {
			return new Embedded412("请填写转让手机号");
		}

		Long transferUserId =
				membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, tm);
		if (transferUserId == null) {
			return new Embedded412("当前转让手机号不是会员");
		}

		int logTs = (int) (System.currentTimeMillis() / 1000);
		RightsTransferLogs logRow = new RightsTransferLogs();
		logRow.setRightsId(rightsId);
		logRow.setUserId(userId);
		logRow.setTransferUserId(transferUserId);
		logRow.setMobile(mobile);
		logRow.setTransferMobile(tm);
		logRow.setCompanyId(companyId);
		logRow.setRemark(remark);
		logRow.setCreated(logTs);
		logRow.setUpdated(logTs);
		rightsTransferLogsMapper.insert(logRow);

		int rightsTs = (int) (System.currentTimeMillis() / 1000);
		LambdaUpdateWrapper<Rights> uw =
				Wrappers.<Rights>lambdaUpdate()
						.eq(Rights::getRightsId, rightsId)
						.eq(Rights::getCompanyId, companyId)
						.set(Rights::getMobile, tm)
						.set(Rights::getUserId, transferUserId)
						.set(Rights::getUpdated, rightsTs);
		int rows = rightsMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("权益不存在");
		}
		if (rows != 1) {
			throw new ResourceException("更新权益失败");
		}

		return new Success();
	}
}
