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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DistributorMenuPermissionService {

	public static final String ROUTE_ALIAS_DISTRIBUTOR_CREATE = "distributor.create";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_ITEM_CREATE = "distributor.item.create";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_ITEM_LIST = "distributor.item.list";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_ITEM_DELETE = "distributor.item.delete";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_ITEM_UPDATE = "distributor.item.update";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_ITEM_EXPORTLIST = "distributor.item.exportlist";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_EDIT = "distributor.edit";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_DEFAULT_SET = "distributor.default.set";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_LIST = "distributor.list";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_INFO = "distributor.info";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_PAYMENT_SUBJECT_SET = "distributor.payment.subject.set";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_COUNT = "front.wxapp.distributor.count";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_WXACODE = "distributor.wxacode";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_EASY_LIST = "distributor.easy.list";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_ADD = "distributor.tag.add";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_DELETE = "distributor.tag.delete";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_UPDATE = "distributor.tag.update";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_LIST = "distributor.tag.list";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_GET = "distributor.tag.get";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_REL = "distributor.tag.rel";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_TAG_DEL = "distributor.tag.del";

	public static final String ROUTE_ALIAS_DISTRIBUTION_BASIC_CONFIG_SAVE = "distribution.basic_config.save";

	public static final String ROUTE_ALIAS_DISTRIBUTION_DISTANCE_SAVE = "distribution.distance.save";

	public static final String ROUTE_ALIAS_DISTRIBUTION_DISTANCE_GET = "distribution.distance.get";

	public static final String ROUTE_ALIAS_DISTRIBUTION_CASH_WITHDRAWAL_LIST = "distribution.cash_withdrawal.list";

	public static final String ROUTE_ALIAS_FRONT_WXAPP_DISTRIBUTION_LOG = "front.wxapp.distribution.log";

	public static final String ROUTE_ALIAS_FRONT_WXAPP_DISTRIBUTION_COUNT = "front.wxapp.distribution.count";

	public static final String ROUTE_ALIAS_DISTRIBUTION_CASH_WITHDRAWAL_PROCESS = "distribution.cash_withdrawal.process";

	public static final String ROUTE_ALIAS_FRONT_WXAPP_CASH_WITHDRAWAL_PAYINFO = "front.wxapp.cashWithdrawal.payinfo";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_BASIC_CONFIG_GET = "distributor.basic_config.get";

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

	public DistributorMenuPermissionService(
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService) {
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
	}

	public void assertRouteAllowed(Map<String, Object> user, String routeAlias) {
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
					&& (OPERATOR_SELECT_DISTRIBUTOR.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_LIST.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_INFO.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_WXACODE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_COUNT.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_CREATE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_EDIT.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_DEFAULT_SET.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_ITEM_CREATE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_ITEM_LIST.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_ITEM_DELETE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_ITEM_UPDATE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_ITEM_EXPORTLIST.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_PAYMENT_SUBJECT_SET.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_EASY_LIST.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_ADD.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_DELETE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_UPDATE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_LIST.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_GET.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_REL.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_TAG_DEL.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTION_BASIC_CONFIG_SAVE.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTION_CASH_WITHDRAWAL_LIST.equals(routeAlias)
							|| ROUTE_ALIAS_FRONT_WXAPP_DISTRIBUTION_LOG.equals(routeAlias)
							|| ROUTE_ALIAS_FRONT_WXAPP_DISTRIBUTION_COUNT.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTION_CASH_WITHDRAWAL_PROCESS.equals(routeAlias)
							|| ROUTE_ALIAS_FRONT_WXAPP_CASH_WITHDRAWAL_PAYINFO.equals(routeAlias)
							|| ROUTE_ALIAS_DISTRIBUTOR_BASIC_CONFIG_GET.equals(routeAlias))) {
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
		Object operatorIdRaw = user.get("operator_id");
		List<String> aliases;
		if (operatorIdRaw == null) {
			aliases = null;
		} else {
			aliases = operatorRoleMenuAliasService.listShopMenuAliases(companyId, longOf(operatorIdRaw));
		}
		if (aliases == null && "staff".equals(operatorType)) {
			throw new ResourceException("帐号没有绑定角色，请联系管理员添加");
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
