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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatacubeShopRoutePermissionService {

	public static final String ROUTE_ALIAS_SOURCE_CREATE = "source.create";

	public static final String ROUTE_ALIAS_SOURCE_DELETE = "source.delete";

	public static final String ROUTE_ALIAS_SOURCE_UPDATE = "source.update";

	public static final String ROUTE_ALIAS_SOURCE_SAVETAGS = "source.savetags";

	public static final String ROUTE_ALIAS_SOURCE_LIST = "source.list";

	public static final String ROUTE_ALIAS_SOURCE_DETAIL = "source.detail";

	public static final String ROUTE_ALIAS_MONITORS_ADD = "monitors.add";

	public static final String ROUTE_ALIAS_MONITORS_LIST = "monitors.list";

	public static final String ROUTE_ALIAS_MONITORS_DELETE = "monitors.delete";

	public static final String ROUTE_ALIAS_MONITORS_RELSOURCES_DETAIL = "monitors.relsources.detail";

	public static final String ROUTE_ALIAS_MONITORS_RELSOURCES_LIST = "monitors.relsources.list";

	public static final String ROUTE_ALIAS_MONITORS_RELSOURCES_DELETE = "monitors.relsources.delete";

	private static final String OPERATOR_SELECT_DISTRIBUTOR = "operator.select.distributor";

	private static final Set<String> PATH_WHITELIST =
			Set.of("companys.setting", "account.roles.permission", "operator.get.data", "currency.default");

	/**
	 * PHP {@code routes/api/datacube.php} registers source APIs with only {@code api.auth}, {@code activated},
	 * {@code shoplog} — no shop-menu alias gate. When granular superadmin permission is off, match that behavior.
	 */
	private static final Set<String> DATACUBE_SOURCE_ROUTE_ALIASES = Set.of(
			ROUTE_ALIAS_SOURCE_CREATE,
			ROUTE_ALIAS_SOURCE_DELETE,
			ROUTE_ALIAS_SOURCE_UPDATE,
			ROUTE_ALIAS_SOURCE_SAVETAGS,
			ROUTE_ALIAS_SOURCE_LIST,
			ROUTE_ALIAS_SOURCE_DETAIL);
	public static final String ROUTE_ALIAS_DELIVERY_STAFF_DATA_EXPORT = "datacube.deliverystaff.data.export";

	public static final String ROUTE_ALIAS_DELIVERY_STAFF_DATA = "datacube.deliverystaff.data";

	public static final String ROUTE_ALIAS_COMPANY_DATA = "datacube.company.data";

	public static final String ROUTE_ALIAS_DISTRIBUTOR_DATA = "datacube.distributor.data";

	public static final String ROUTE_ALIAS_GOODS_DATA = "datacube.goods.data";

	public static final String ROUTE_ALIAS_MINIPROGRAM_PAGES = "miniprogram.pages";

	private static final Set<String> DELIVERY_STAFF_PATHS = Set.of(
			"order.deliverypackag.confirm",
			"order.deliverystaff.confirm",
			"order.deliverystaff.cancel",
			ROUTE_ALIAS_DELIVERY_STAFF_DATA,
			ROUTE_ALIAS_DELIVERY_STAFF_DATA_EXPORT);

	private final OperatorRoleMenuAliasService operatorRoleMenuAliasService;
	private final ShopMenuService shopMenuService;

	@Value("${common.check-superadmin-permission:false}")
	private boolean checkSuperadminPermission;

	public DatacubeShopRoutePermissionService(
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService) {
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
	}

	public void assertSourceCreate(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_CREATE);
	}

	public void assertSourceDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_DELETE);
	}

	public void assertSourceUpdate(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_UPDATE);
	}

	public void assertSourceSaveTags(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_SAVETAGS);
	}

	public void assertSourceList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_LIST);
	}

	public void assertSourceDetail(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SOURCE_DETAIL);
	}

	public void assertMonitorsAdd(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_ADD);
	}

	public void assertMonitorsList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_LIST);
	}

	public void assertMonitorsDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_DELETE);
	}

	public void assertMonitorsRelSourcesDetail(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_RELSOURCES_DETAIL);
	}

	public void assertMonitorsRelSourcesList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_RELSOURCES_LIST);
	}

	public void assertMonitorsRelSourcesDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MONITORS_RELSOURCES_DELETE);
	}

	public void assertDeliveryStaffDataExport(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DELIVERY_STAFF_DATA_EXPORT);
	}

	public void assertDeliveryStaffData(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DELIVERY_STAFF_DATA);
	}

	public void assertCompanyData(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_COMPANY_DATA);
	}

	public void assertDistributorData(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_DISTRIBUTOR_DATA);
	}

	public void assertGoodsData(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_GOODS_DATA);
	}

	public void assertMiniprogramPages(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_MINIPROGRAM_PAGES);
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
		if (!checkSuperadminPermission && DATACUBE_SOURCE_ROUTE_ALIASES.contains(routeAlias)) {
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
