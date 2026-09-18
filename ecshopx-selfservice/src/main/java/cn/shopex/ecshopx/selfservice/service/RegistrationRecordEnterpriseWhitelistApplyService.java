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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordEnterpriseWhitelistApplyService {

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MemberAccountService memberAccountService;
	private final EnterprisesMapper enterprisesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final RegistrationRecordMapper registrationRecordMapper;

	public RegistrationRecordEnterpriseWhitelistApplyService(
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MemberAccountService memberAccountService,
			EnterprisesMapper enterprisesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			RegistrationRecordMapper registrationRecordMapper) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.memberAccountService = memberAccountService;
		this.enterprisesMapper = enterprisesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.registrationRecordMapper = registrationRecordMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void applyWhiteListForPassedRegistration(RegistrationRecord recordSnapshot, RegistrationActivity activityInfo) {
		long companyId = recordSnapshot.getCompanyId();
		LambdaUpdateWrapper<RegistrationRecord> markWhite = new LambdaUpdateWrapper<>();
		markWhite
				.eq(RegistrationRecord::getCompanyId, companyId)
				.eq(RegistrationRecord::getRecordId, recordSnapshot.getRecordId())
				.set(RegistrationRecord::getIsWhiteList, 1L);
		registrationRecordMapper.update(null, markWhite);

		String dm = Objects.toString(sensitiveFieldEncryptor.decrypt(recordSnapshot.getMobile() == null ? "" : recordSnapshot.getMobile()), "");
		String dfm = Objects.toString(
				sensitiveFieldEncryptor.decrypt(recordSnapshot.getFormMobile() == null ? "" : recordSnapshot.getFormMobile()), "");

		final long userId;
		final String workingMemberMobile;
		if (Objects.equals(dm, dfm)) {
			userId = recordSnapshot.getUserId() != null ? recordSnapshot.getUserId() : 0L;
			workingMemberMobile = dm;
		} else {
			Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, dfm);
			if (m != null) {
				userId = m.getUserId() != null ? m.getUserId() : 0L;
				workingMemberMobile = dfm;
			} else {
				userId = 0L;
				workingMemberMobile = "";
			}
		}

		String storedMobile = memberAccountService.encodeMobileForStorage(dfm);
		String[] enterpriseIds = activityInfo.getEnterpriseIds().split(",");
		int now = (int) (System.currentTimeMillis() / 1000L);
		String trueName = recordSnapshot.getTrueName() != null ? recordSnapshot.getTrueName() : "";

		for (String rawEid : enterpriseIds) {
			if (rawEid == null) {
				continue;
			}
			String tid = rawEid.trim();
			if (!StringUtils.hasText(tid)) {
				continue;
			}
			long eid;
			try {
				eid = Long.parseLong(tid);
			} catch (NumberFormatException ex) {
				continue;
			}
			if (eid <= 0L) {
				continue;
			}

			Enterprises ent = enterprisesMapper.selectById(eid);
			if (ent == null) {
				continue;
			}

			Employees candidate = new Employees();
			candidate.setCompanyId(companyId);
			candidate.setEnterpriseId(eid);
			candidate.setName(trueName);
			candidate.setUserId(userId);
			candidate.setMemberMobile(workingMemberMobile);
			candidate.setMobile(dfm);
			candidate.setDistributorId(ent.getDistributorId() != null ? ent.getDistributorId() : 0);
			candidate.setCreated(now);
			candidate.setUpdated(now);

			Employees existing;
			if (userId > 0L) {
				existing =
						employeesMapper.selectOne(
								new LambdaQueryWrapper<Employees>()
										.eq(Employees::getCompanyId, companyId)
										.eq(Employees::getUserId, userId)
										.eq(Employees::getEnterpriseId, eid)
										.last("LIMIT 1"));
			} else {
				existing =
						employeesMapper.selectOne(
								new LambdaQueryWrapper<Employees>()
										.eq(Employees::getCompanyId, companyId)
										.eq(Employees::getMobile, storedMobile)
										.eq(Employees::getEnterpriseId, eid)
										.last("LIMIT 1"));
			}
			if (existing == null) {
				employeesMapper.insert(candidate);
			}

			if (userId > 0L) {
				LambdaUpdateWrapper<Relatives> ruw = new LambdaUpdateWrapper<>();
				ruw.set(Relatives::getDisabled, true)
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getUserId, userId)
						.eq(Relatives::getEnterpriseId, eid);
				relativesMapper.update(null, ruw);
			}
		}
	}
}
