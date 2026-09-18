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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonPushPort.OpenapiSalespersonPushInput;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OpenapiThirdApiV1SalespersonPushService {

	private static final String SALESPERSON_TYPE = "shopping_guide";
	private static final Pattern MOBILE_REGEX = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final String MSG_NAME_REQUIRED = "请填写导购名称";
	private static final String MSG_EMPLOYEE_STATUS = "请填写正确的员工类型";
	private static final String MSG_MOBILE = "请填写正确的手机号";
	private static final String MSG_WORK_USERID = "请填写企业微信userid";
	private static final String MSG_SALESPERSON_STATUS = "请填写是否启用";
	private static final String MSG_STORE_NOT_FOUND = "店铺code未查询到正在开启的店铺信息";
	private static final String MSG_UPDATE_NOT_FOUND = "更新的人员不存在";

	private final DistributorListQueryService distributorListQueryService;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV1SalespersonPushService(
			DistributorListQueryService distributorListQueryService,
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			TransactionTemplate transactionTemplate) {
		this.distributorListQueryService = distributorListQueryService;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> executePushSalesperson(long companyId, OpenapiSalespersonPushInput input) {
		validateParams(input);

		String salespersonStatusRaw = input.salespersonStatusRaw().trim();
		String isValid = "1".equals(salespersonStatusRaw) ? "true" : "false";
		int employeeStatus = Integer.parseInt(input.employeeStatusRaw().trim());

		String name = input.salespersonName().trim();
		String workUserid = input.workUserid().trim();
		String mobilePlain =
				input.mobilePresent() && input.mobileRaw() != null ? input.mobileRaw().trim() : null;
		String storeBn = input.storeBn() != null ? input.storeBn() : "";
		String salespersonJob = input.salespersonJob() != null ? input.salespersonJob() : "";
		String workQrcodeConfigid = input.workQrcodeConfigid() != null ? input.workQrcodeConfigid() : "";

		Map<String, Object> data = formatSalesperson(
				companyId,
				name,
				employeeStatus,
				salespersonJob,
				input.salespersonAvatar(),
				mobilePlain,
				input.mobilePresent(),
				workUserid,
				isValid,
				workQrcodeConfigid,
				storeBn);

		ShopSalesperson existing = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonType, SALESPERSON_TYPE)
				.eq(ShopSalesperson::getWorkUserid, workUserid)
				.last("LIMIT 1"));

		if (existing != null) {
			if (input.newUserIdPresent()
					&& input.newUserIdTruthy()
					&& input.newUserIdRaw() != null
					&& !input.newUserIdRaw().equals(workUserid)) {
				data.put("work_userid", input.newUserIdRaw());
			}
			return updateSalesperson(companyId, existing.getSalespersonId(), data, input.mobilePresent());
		}
		return createSalesperson(data, input.mobilePresent());
	}

	private void validateParams(OpenapiSalespersonPushInput input) {
		String salespersonName = input.salespersonName();
		if (salespersonName == null || !StringUtils.hasText(salespersonName.trim())) {
			throw new ResourceException(MSG_NAME_REQUIRED);
		}

		String employeeStatusRaw = input.employeeStatusRaw();
		if (employeeStatusRaw == null
				|| !StringUtils.hasText(employeeStatusRaw.trim())
				|| !isEmployeeStatusValid(employeeStatusRaw.trim())) {
			throw new ResourceException(MSG_EMPLOYEE_STATUS);
		}

		if (input.mobilePresent()) {
			String mobileRaw = input.mobileRaw();
			if (mobileRaw == null
					|| !StringUtils.hasText(mobileRaw.trim())
					|| !MOBILE_REGEX.matcher(mobileRaw.trim()).matches()) {
				throw new ResourceException(MSG_MOBILE);
			}
		}

		String workUserid = input.workUserid();
		if (workUserid == null || !StringUtils.hasText(workUserid.trim())) {
			throw new ResourceException(MSG_WORK_USERID);
		}

		String salespersonStatusRaw = input.salespersonStatusRaw();
		if (salespersonStatusRaw == null
				|| !StringUtils.hasText(salespersonStatusRaw.trim())
				|| !isSalespersonStatusValid(salespersonStatusRaw.trim())) {
			throw new ResourceException(MSG_SALESPERSON_STATUS);
		}
	}

	private static boolean isEmployeeStatusValid(String trimmed) {
		return "1".equals(trimmed) || "2".equals(trimmed);
	}

	private static boolean isSalespersonStatusValid(String trimmed) {
		return "0".equals(trimmed) || "1".equals(trimmed) || "2".equals(trimmed);
	}

	private Map<String, Object> formatSalesperson(
			long companyId,
			String name,
			int employeeStatus,
			String salespersonJob,
			String avatar,
			String mobilePlain,
			boolean mobilePresent,
			String workUserid,
			String isValid,
			String workQrcodeConfigid,
			String storeBn) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("name", name);
		data.put("salesperson_type", SALESPERSON_TYPE);
		data.put("employee_status", employeeStatus);
		data.put("salesperson_job", salespersonJob != null ? salespersonJob : "");
		data.put("avatar", avatar);
		data.put("mobile", mobilePresent ? mobilePlain : null);
		data.put("work_userid", workUserid);
		data.put("is_valid", isValid);
		data.put("work_qrcode_configid", workQrcodeConfigid != null ? workQrcodeConfigid : "");

		String shopCode = storeBn != null ? storeBn.trim() : "";
		if (StringUtils.hasText(shopCode)) {
			List<Distributor> list = distributorListQueryService
					.listValidByCompanyAndShopCodesOrderedByCreatedDesc(companyId, List.of(shopCode));
			if (list.isEmpty()) {
				log.info("openapi createSalesperson error:店铺错误:{},work_userid:{}", shopCode, workUserid);
				throw new ResourceException(MSG_STORE_NOT_FOUND);
			}
			data.put("distributor_id", list.stream().map(Distributor::getDistributorId).toList());
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> createSalesperson(Map<String, Object> data, boolean mobilePresent) {
		List<Long> distributorIds = (List<Long>) data.remove("distributor_id");

		long companyId = ((Number) data.get("company_id")).longValue();
		String namePlain = (String) data.get("name");
		Integer employeeStatus = (Integer) data.get("employee_status");
		String salespersonJob = (String) data.get("salesperson_job");
		String avatar = (String) data.get("avatar");
		String workUserid = (String) data.get("work_userid");
		String isValid = (String) data.get("is_valid");
		String workQrcodeConfigid = (String) data.get("work_qrcode_configid");
		String mobilePlain = mobilePresent ? (String) data.get("mobile") : null;

		ShopSalesperson row = new ShopSalesperson();
		row.setName(sensitiveFieldEncryptor.encrypt(namePlain));
		if (mobilePresent && mobilePlain != null) {
			row.setMobile(sensitiveFieldEncryptor.encrypt(mobilePlain.trim()));
		}
		row.setCompanyId(companyId);
		row.setUserId(0);
		row.setSalespersonType(SALESPERSON_TYPE);
		row.setNumber("");
		row.setRole("0");
		row.setEmployeeStatus(employeeStatus);
		row.setShopId("0");
		row.setWorkUserid(workUserid);
		row.setIsValid(isValid);
		row.setAvatar(avatar);
		row.setWorkQrcodeConfigid(workQrcodeConfigid);
		row.setSalespersonJob(salespersonJob);
		row.setChildCount(0);
		row.setFriendCount(0);
		long epochSecond = Instant.now().getEpochSecond();
		row.setCreatedTime(String.valueOf(epochSecond));
		row.setCreated(epochSecond);
		row.setUpdated(epochSecond);

		final List<Long> finalDistributorIds = distributorIds;
		transactionTemplate.executeWithoutResult(status -> {
			shopSalespersonMapper.insert(row);
			if (finalDistributorIds != null && !finalDistributorIds.isEmpty()) {
				for (Long did : finalDistributorIds) {
					if (did == null) {
						continue;
					}
					ShopsRelSalesperson rel = new ShopsRelSalesperson();
					rel.setShopId(did);
					rel.setSalespersonId(row.getSalespersonId());
					rel.setCompanyId(companyId);
					rel.setStoreType("distributor");
					shopsRelSalespersonMapper.insert(rel);
				}
			}
		});

		ShopSalesperson refreshed = shopSalespersonMapper.selectById(row.getSalespersonId());
		if (refreshed == null) {
			throw new ResourceException("同步导购数据失败");
		}
		return DistributorShoppingGuideAddService.buildMobileFindDataMap(refreshed, sensitiveFieldEncryptor);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> updateSalesperson(
			long companyId, long salespersonId, Map<String, Object> data, boolean mobilePresent) {
		List<Long> distributorIds = (List<Long>) data.remove("distributor_id");
		data.remove("shop_id");

		String namePlain = (String) data.get("name");
		String avatar = (String) data.get("avatar");
		String workUserid = (String) data.get("work_userid");
		String isValid = (String) data.get("is_valid");
		Integer employeeStatus = (Integer) data.get("employee_status");
		String salespersonJob = (String) data.get("salesperson_job");
		String workQrcodeConfigid = (String) data.get("work_qrcode_configid");
		String mobilePlain = mobilePresent ? (String) data.get("mobile") : null;

		final List<Long> finalDistributorIds = distributorIds;
		transactionTemplate.executeWithoutResult(status -> {
			long now = Instant.now().getEpochSecond();
			LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
					.eq(ShopSalesperson::getSalespersonId, salespersonId)
					.eq(ShopSalesperson::getCompanyId, companyId)
					.eq(ShopSalesperson::getSalespersonType, SALESPERSON_TYPE)
					.set(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(namePlain))
					.set(ShopSalesperson::getAvatar, avatar)
					.set(ShopSalesperson::getWorkUserid, workUserid)
					.set(ShopSalesperson::getIsValid, isValid)
					.set(ShopSalesperson::getEmployeeStatus, employeeStatus)
					.set(ShopSalesperson::getSalespersonJob, salespersonJob)
					.set(ShopSalesperson::getWorkQrcodeConfigid, workQrcodeConfigid)
					.set(ShopSalesperson::getUpdated, now);
			if (mobilePresent && mobilePlain != null) {
				uw.set(ShopSalesperson::getMobile, sensitiveFieldEncryptor.encrypt(mobilePlain.trim()));
			}
			int rows = shopSalespersonMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException(MSG_UPDATE_NOT_FOUND);
			}
			if (finalDistributorIds != null && !finalDistributorIds.isEmpty()) {
				shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, salespersonId));
				for (Long did : finalDistributorIds) {
					if (did == null) {
						continue;
					}
					ShopsRelSalesperson rel = new ShopsRelSalesperson();
					rel.setShopId(did);
					rel.setSalespersonId(salespersonId);
					rel.setCompanyId(companyId);
					rel.setStoreType("distributor");
					shopsRelSalespersonMapper.insert(rel);
				}
			}
		});

		ShopSalesperson refreshed = shopSalespersonMapper.selectById(salespersonId);
		if (refreshed == null) {
			throw new ResourceException(MSG_UPDATE_NOT_FOUND);
		}
		return DistributorShoppingGuideAddService.buildMobileFindDataMap(refreshed, sensitiveFieldEncryptor);
	}
}
