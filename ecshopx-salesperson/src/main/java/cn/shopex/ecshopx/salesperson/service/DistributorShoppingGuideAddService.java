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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.service.PromoterShopStatusSalespersonService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class DistributorShoppingGuideAddService {

	private final MemberAccountService memberAccountService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final PromoterShopStatusSalespersonService promoterShopStatusSalespersonService;
	private final TransactionTemplate transactionTemplate;

	public DistributorShoppingGuideAddService(MemberAccountService memberAccountService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor, ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			PromoterShopStatusSalespersonService promoterShopStatusSalespersonService,
			TransactionTemplate transactionTemplate) {
		this.memberAccountService = memberAccountService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.promoterShopStatusSalespersonService = promoterShopStatusSalespersonService;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> addSalesman(long companyId, String mobileRaw, String salesmanNameRaw,
			String[] distributorIdParamValues, String roleRaw, String employeeStatusRaw) {
		if (mobileRaw == null || !StringUtils.hasText(mobileRaw.trim())) {
			throw new BadRequestException("请填写手机号", Map.of("mobile", List.of("请填写手机号")), 422);
		}
		String mobileTrim = mobileRaw.trim();
		if (salesmanNameRaw == null || !StringUtils.hasText(salesmanNameRaw.trim())) {
			throw new BadRequestException("请填写导购员姓名",
					Map.of("salesman_name", List.of("请填写导购员姓名")), 422);
		}
		String salesmanNameTrim = salesmanNameRaw.trim();
		if (distributorIdParamValues == null || distributorIdParamValues.length == 0) {
			throw new BadRequestException("请选择导购员所属店铺",
					Map.of("distributor_id", List.of("请选择导购员所属店铺")), 422);
		}
		boolean anyDistributor = false;
		for (String s : distributorIdParamValues) {
			if (s != null && StringUtils.hasText(s.trim())) {
				anyDistributor = true;
				break;
			}
		}
		if (!anyDistributor) {
			throw new BadRequestException("请选择导购员所属店铺",
					Map.of("distributor_id", List.of("请选择导购员所属店铺")), 422);
		}

		Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileTrim);
		if (m == null) {
			throw new ResourceException("当前手机号还不是会员");
		}

		Map<String, Object> dist = distributorRepositoryGetInfoSimpleService.resolveDistributorForAddSalesman(
				companyId, distributorIdParamValues);
		if (!Boolean.TRUE.equals(dist.get("is_open_salesman"))) {
			throw new ResourceException("店铺未开启业务员配置！");
		}

		Object didObj = dist.get("distributor_id");
		if (!(didObj instanceof Number)) {
			throw new ResourceException("店铺未开启业务员配置！");
		}
		long resolvedShopId = ((Number) didObj).longValue();
		String shopIdStr = String.valueOf(resolvedShopId);

		Long memberUserId = m.getUserId();
		if (memberUserId == null) {
			throw new ResourceException("当前手机号还不是会员");
		}

		ShopSalesperson dupUser = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getUserId, memberUserId.intValue())
				.eq(ShopSalesperson::getShopId, shopIdStr)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (dupUser != null) {
			throw new ResourceException("当前会员已添加！");
		}

		String encMobile = sensitiveFieldEncryptor.encrypt(mobileTrim);
		ShopSalesperson dupMobile = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getMobile, encMobile)
				.eq(ShopSalesperson::getShopId, shopIdStr)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (dupMobile != null) {
			throw new ResourceException("当前手机号已绑定");
		}

		String roleStr = String.valueOf(parseLongFromLooseString(roleRaw));
		int employeeStatus = parseEmployeeStatus(employeeStatusRaw);

		ShopSalesperson row = new ShopSalesperson();
		row.setName(sensitiveFieldEncryptor.encrypt(salesmanNameTrim));
		row.setMobile(encMobile);
		row.setCompanyId(companyId);
		row.setUserId(memberUserId.intValue());
		row.setSalespersonType("shopping_guide");
		row.setNumber("");
		row.setRole(roleStr);
		row.setEmployeeStatus(employeeStatus);
		row.setShopId(shopIdStr);
		long epochSecond = Instant.now().getEpochSecond();
		row.setCreatedTime(String.valueOf(epochSecond));
		row.setCreated(epochSecond);
		row.setUpdated(epochSecond);
		row.setIsValid("true");

		transactionTemplate.executeWithoutResult(status -> {
			shopSalespersonMapper.insert(row);
			if (!"0".equals(shopIdStr) && resolvedShopId > 0L) {
				ShopsRelSalesperson rel = new ShopsRelSalesperson();
				rel.setShopId(resolvedShopId);
				rel.setSalespersonId(row.getSalespersonId());
				rel.setCompanyId(companyId);
				rel.setStoreType("shop");
				shopsRelSalespersonMapper.insert(rel);
			}
		});

		promoterShopStatusSalespersonService.updateShopStatusSalesperson(companyId, memberUserId);

		return buildResponseMap(row);
	}

	static Map<String, Object> buildMobileFindDataMap(ShopSalesperson row, SensitiveFieldEncryptor enc) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(out, row, enc);
		out.put("role", row.getRole() != null ? row.getRole() : "");
		appendPayloadFieldsAfterRole(out, row);
		return out;
	}

	private Map<String, Object> buildResponseMap(ShopSalesperson row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		appendPayloadFieldsBeforeRole(out, row, sensitiveFieldEncryptor);
		out.put("role", parseRoleAsInt(row.getRole()));
		appendPayloadFieldsAfterRole(out, row);
		return out;
	}

	private static void appendPayloadFieldsBeforeRole(LinkedHashMap<String, Object> out, ShopSalesperson row,
			SensitiveFieldEncryptor enc) {
		out.put("salesperson_id", String.valueOf(row.getSalespersonId()));
		out.put("name", row.getName() != null ? enc.decrypt(row.getName()) : "");
		out.put("mobile", row.getMobile() != null ? enc.decrypt(row.getMobile()) : "");
		out.put("created_time", resolveCreatedTimeEpochSeconds(row));
		out.put("salesperson_type", row.getSalespersonType() != null ? row.getSalespersonType() : "");
		out.put("company_id", String.valueOf(row.getCompanyId()));
		out.put("user_id", String.valueOf(row.getUserId()));
		out.put("child_count", row.getChildCount() != null ? row.getChildCount() : 0);
		out.put("is_valid", row.getIsValid() != null ? row.getIsValid() : "");
		out.put("shop_id", row.getShopId() != null ? row.getShopId() : "");
		out.put("shop_name", row.getShopName());
		out.put("number", row.getNumber() != null ? row.getNumber() : "");
		out.put("friend_count", row.getFriendCount() != null ? row.getFriendCount() : 0);
		out.put("avatar", row.getAvatar());
		out.put("work_userid", row.getWorkUserid());
		out.put("work_configid", row.getWorkConfigid());
		out.put("work_qrcode_configid", row.getWorkQrcodeConfigid());
	}

	private static void appendPayloadFieldsAfterRole(LinkedHashMap<String, Object> out, ShopSalesperson row) {
		out.put("salesperson_job", row.getSalespersonJob() != null ? row.getSalespersonJob() : "");
		out.put("employee_status", row.getEmployeeStatus() != null ? row.getEmployeeStatus() : 0);
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		out.put("work_clear_userid", row.getWorkClearUserid());
	}

	private static long resolveCreatedTimeEpochSeconds(ShopSalesperson row) {
		String ct = row.getCreatedTime();
		if (ct != null && !ct.trim().isEmpty()) {
			try {
				return Long.parseLong(ct.trim());
			} catch (NumberFormatException ignored) {
				// fall through to created
			}
		}
		Long created = row.getCreated();
		return created != null ? created : 0L;
	}

	private static int parseRoleAsInt(String role) {
		if (role == null) {
			return 0;
		}
		String t = role.trim();
		if (t.isEmpty()) {
			return 0;
		}
		long v = LeadingNumberParser.parseAsLong(t);
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}

	private static long parseLongFromLooseString(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		return LeadingNumberParser.parseAsLong(t);
	}

	private static int parseEmployeeStatus(String employeeStatusRaw) {
		if (employeeStatusRaw == null) {
			return 1;
		}
		long v = parseLongFromLooseString(employeeStatusRaw);
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
