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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.dispatch.CompanysDispatchEventNames;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.activation.CompanyDemoLicenseService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class OperatorsOpenService {

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	@Value("${common.system-companys-id:0}")
	private long systemCompanysId;

	private final OperatorsMapper operatorsMapper;
	private final CompanysMapper companysMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final ShopMenuService shopMenuService;
	private final CompanyDemoLicenseService companyDemoLicenseService;
	private final DispatchFacade dispatchFacade;

	public OperatorsOpenService(
			OperatorsMapper operatorsMapper,
			CompanysMapper companysMapper,
			OperatorsQueryService operatorsQueryService,
			ShopMenuService shopMenuService,
			CompanyDemoLicenseService companyDemoLicenseService,
			DispatchFacade dispatchFacade) {
		this.operatorsMapper = operatorsMapper;
		this.companysMapper = companysMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.shopMenuService = shopMenuService;
		this.companyDemoLicenseService = companyDemoLicenseService;
		this.dispatchFacade = dispatchFacade;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> open(Map<String, Object> operatorData) {
		String mobile = str(operatorData.get("mobile"));
		Map<String, Object> existing =
				operatorsQueryService.getInfo(
						new HashMap<>(
								Map.of("mobile", mobile, "operator_type", "admin")));
		if (existing != null && !existing.isEmpty()) {
			throw new ResourceException("账号已开通");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Operators op = new Operators();
		op.setMobile(mobile);
		op.setOperatorType("admin");
		op.setEid(str(operatorData.get("eid")));
		op.setPassportUid(str(operatorData.get("passport_uid")));
		op.setLoginName("");
		String plainPwd = str(operatorData.get("password"));
		if (StringUtils.hasText(plainPwd)) {
			op.setPassword(BCrypt.hashpw(plainPwd, BCrypt.gensalt()));
		} else {
			op.setPassword("");
		}
		op.setCompanyId(0L);
		op.setCreated(now);
		op.setUpdated(now);
		operatorsMapper.insert(op);
		long operatorId = op.getOperatorId();

		long expiredAt = 9999999999L;

		Companys company = new Companys();
		company.setCompanyName("");
		company.setEid(str(operatorData.get("eid")));
		company.setPassportUid(str(operatorData.get("passport_uid")));
		company.setCompanyAdminOperatorId(operatorId);
		company.setExpiredAt(expiredAt);
		company.setIsDisabled(false);
		company.setThirdParams("{}");
		company.setCreated(now);
		company.setUpdated(now);
		String menuKey = str(operatorData.get("menu_type"));
		company.setMenuType(shopMenuService.productModelKeyToMenuTypeInt(menuKey));
		if (!systemIsSaas) {
			company.setCompanyId(systemCompanysId);
		}
		companysMapper.insert(company);
		long companyId = company.getCompanyId();

		Operators opUpd = new Operators();
		opUpd.setOperatorId(operatorId);
		opUpd.setCompanyId(companyId);
		opUpd.setUpdated(now);
		operatorsMapper.updateById(opUpd);

		LinkedHashMap<String, Object> demoParams = new LinkedHashMap<>();
		demoParams.put("eid", str(operatorData.get("eid")));
		demoParams.put("passport_uid", str(operatorData.get("passport_uid")));
		demoParams.put("company_id", companyId);
		demoParams.put("expired_at", expiredAt);
		companyDemoLicenseService.createDemoCompanyLicense(demoParams);

		long activeAt = System.currentTimeMillis() / 1000L;
		Long expRow = company.getExpiredAt();
		long notifyExpiredAt = expRow != null ? expRow : expiredAt;
		String issueId = strNullable(operatorData.get("issue_id"));
		String notifyEmail = strNullable(operatorData.get("email"));

		LinkedHashMap<String, Object> eventPayload = new LinkedHashMap<>();
		eventPayload.put("company_id", companyId);
		eventPayload.put("issue_id", issueId);
		eventPayload.put("sms_mobile", mobile);
		eventPayload.put("notify_email", notifyEmail);
		eventPayload.put("active_at_epoch_seconds", activeAt);
		eventPayload.put("expired_at_epoch_seconds", notifyExpiredAt);
		schedulePublishCompanyCreateAfterCommit(eventPayload);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("operator_id", operatorId);
		out.put("company_id", companyId);
		out.put("mobile", mobile);
		return out;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String strNullable(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private void schedulePublishCompanyCreateAfterCommit(LinkedHashMap<String, Object> eventPayload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publishCompanyCreate(eventPayload);
						}
					});
		} else {
			publishCompanyCreate(eventPayload);
		}
	}

	private void publishCompanyCreate(LinkedHashMap<String, Object> eventPayload) {
		dispatchFacade.publishEvent(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				eventPayload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));
	}
}
