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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DepositShopRoutePermissionService {

	public static final String ROUTE_ALIAS_RECHARGE_RULE_CREATE = "deposit.rule.create";

	public static final String ROUTE_ALIAS_RECHARGE_RULE_EDIT = "deposit.rule.edit";

	public static final String ROUTE_ALIAS_RECHARGE_RULE_DELETE = "deposit.rule.delete";

	public static final String ROUTE_ALIAS_DEPOSIT_RECHARGE = "deposit.recharge";

	public static final String ROUTE_ALIAS_RECHARGE_AGREEMENT_SET = "deposit.recharge.agreement.set";

	public static final String ROUTE_ALIAS_RECHARGE_AGREEMENT_GET = "deposit.recharge.agreement.get";

	public static final String ROUTE_ALIAS_RECHARGE_MULTIPLE_SET = "deposit.recharge.multiple.set";

	public static final String ROUTE_ALIAS_RECHARGE_MULTIPLE_GET = "deposit.recharge.multiple.get";

	public static final String ROUTE_ALIAS_DEPOSIT_COUNT_INDEX = "deposit.count.index";

	public static final String ROUTE_ALIAS_RECHARGE_RULE_LIST = "deposit.rule.list";

	public static final String ROUTE_ALIAS_DEPOSIT_TRADES = "deposit.trades";

	private static final String OPERATOR_SELECT_DISTRIBUTOR = "operator.select.distributor";

	private static final Set<String> PATH_WHITELIST =
			Set.of("companys.setting", "account.roles.permission", "operator.get.data", "currency.default");
	private static final Set<String> DELIVERY_STAFF_PATHS = Set.of(
			"order.deliverypackag.confirm",
			"order.deliverystaff.confirm",
			"order.deliverystaff.cancel",
			"datacube.deliverystaff.data",
			"datacube.deliverystaff.data.export");

	private final OperatorRoleMenuAliasService operatorRoleMenuAliasService;
	private final ShopMenuService shopMenuService;

	@Value("${common.check-superadmin-permission:false}")
	private boolean checkSuperadminPermission;

	public DepositShopRoutePermissionService(
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService) {
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
	}

	public void assertRechargeRuleCreate(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_RULE_CREATE);
	}

	public void assertRechargeRuleEdit(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_RULE_EDIT);
	}

	public void assertRechargeRuleDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_RULE_DELETE);
	}

	public void assertDepositRecharge(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DEPOSIT_RECHARGE);
	}

	public void assertRechargeAgreementSet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_AGREEMENT_SET);
	}

	public void assertRechargeAgreementGet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_AGREEMENT_GET);
	}

	public void assertRechargeMultipleSet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_MULTIPLE_SET);
	}

	public void assertRechargeMultipleGet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_MULTIPLE_GET);
	}

	public void assertDepositCountIndex(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DEPOSIT_COUNT_INDEX);
	}

	public void assertRechargeRuleList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECHARGE_RULE_LIST);
	}

	public void assertDepositTrades(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DEPOSIT_TRADES);
	}

	private void assertRouteAllowed(Map<String, Object> user, String routeAlias) {
		Object source = user.get("source");
		if (source != null && "salesperson_workwechat".equals(source.toString())) {
			return;
		}
		String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		if (!checkSuperadminPermission) {
			if (operatorType.isEmpty() || "admin".equals(operatorType)) {
				return;
			}
			if ("distributor".equals(operatorType)
					&& (OPERATOR_SELECT_DISTRIBUTOR.equals(routeAlias) || "distributor.list".equals(routeAlias))) {
				return;
			}
		}
		if (PATH_WHITELIST.contains(routeAlias)) {
			return;
		}
		if ("self_delivery_staff".equals(operatorType) && DELIVERY_STAFF_PATHS.contains(routeAlias)) {
			return;
		}
		long companyId = longOf(user.get("company_id"));
		long operatorId = longOf(user.get("operator_id"));
		List<String> aliases = operatorRoleMenuAliasService.listShopMenuAliases(companyId, operatorId);
		if (aliases == null && "staff".equals(operatorType)) {
			throw new ForbiddenException("帐号没有绑定角色，请联系管理员添加");
		}
		if (aliases == null) {
			aliases = List.of();
		}
		int version = menuVersionForOperatorType(operatorType);
		List<String> apis = shopMenuService.collectApisFromMenus(version, aliases);
		if (apis.contains(routeAlias)) {
			return;
		}
		throw new ForbiddenException("您没有此操作的权限");
	}

	private static int menuVersionForOperatorType(String operatorType) {
		if ("distributor".equals(operatorType)) {
			return 3;
		}
		if ("dealer".equals(operatorType)) {
			return 5;
		}
		if ("merchant".equals(operatorType)) {
			return 6;
		}
		if ("supplier".equals(operatorType)) {
			return 7;
		}
		return 1;
	}

	private static long longOf(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
