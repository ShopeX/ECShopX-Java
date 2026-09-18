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

package cn.shopex.ecshopx.companys.service.reservation;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.reservation.port.ReservationRoutePermissionPort;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompanysReservationRoutePermissionPort implements ReservationRoutePermissionPort {

	public static final String ROUTE_ALIAS_RESERVATION_CREATE = "reservation.create";

	public static final String ROUTE_ALIAS_RESERVATION_GET_LIST = "reservation.get.list";

	public static final String ROUTE_ALIAS_RESERVATION_GET_EVERY_DAY_TIME = "reservation.get.everydaytime";

	public static final String ROUTE_ALIAS_RESERVATION_SETTING_SAVE = "reservation.setting.save";

	public static final String ROUTE_ALIAS_RESERVATION_SETTING_GET = "reservation.setting.get";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_ADD = "resource.level.add";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_UPDATE = "resource.level.update";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_DELETE = "resource.level.delete";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_SET_STATUS = "resource.level.set.status";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_GET = "resource.level.get";

	public static final String ROUTE_ALIAS_RESOURCE_LEVEL_LIST = "resource.level.list";

	public static final String ROUTE_ALIAS_SHIFT_TYPE_CREATE = "shift.type.create";

	public static final String ROUTE_ALIAS_SHIFT_TYPE_UPDATE = "shift.type.update";

	public static final String ROUTE_ALIAS_SHIFT_TYPE_GET_LIST = "shift.type.getlist";

	public static final String ROUTE_ALIAS_SHIFT_TYPE_DELETE = "shift.type.delete";

	public static final String ROUTE_ALIAS_WORK_SHIFT_CREATE = "work.shift.create";

	public static final String ROUTE_ALIAS_WORK_SHIFT_UPDATE = "work.shift.update";

	public static final String ROUTE_ALIAS_WORK_SHIFT_DELETE = "work.shift.delete";

	public static final String ROUTE_ALIAS_WORK_SHIFT_GET_WEEKDAY = "work.shift.getweekday";

	public static final String ROUTE_ALIAS_WORK_SHIFT_GET_LIST = "work.shift.getlist";

	public static final String ROUTE_ALIAS_SHIFT_DEFAULT_GET_LIST = "shift.default.getlist";

	public static final String ROUTE_ALIAS_SHIFT_DEFAULT_DELETE = "shift.default.delete";

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

	public CompanysReservationRoutePermissionPort(
			OperatorRoleMenuAliasService operatorRoleMenuAliasService, ShopMenuService shopMenuService) {
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
	}

	@Override
	public void assertReservationCreateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESERVATION_CREATE);
	}

	@Override
	public void assertReservationGetListAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESERVATION_GET_LIST);
	}

	@Override
	public void assertReservationGetEveryDayTimePeriodAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESERVATION_GET_EVERY_DAY_TIME);
	}

	@Override
	public void assertReservationSettingSaveAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESERVATION_SETTING_SAVE);
	}

	@Override
	public void assertReservationSettingGetAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESERVATION_SETTING_GET);
	}

	@Override
	public void assertResourceLevelAddAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_ADD);
	}

	@Override
	public void assertResourceLevelUpdateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_UPDATE);
	}

	@Override
	public void assertResourceLevelDeleteAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_DELETE);
	}

	@Override
	public void assertResourceLevelSetStatusAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_SET_STATUS);
	}

	@Override
	public void assertResourceLevelGetAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_GET);
	}

	@Override
	public void assertResourceLevelListAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_RESOURCE_LEVEL_LIST);
	}

	@Override
	public void assertShiftTypeCreateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_TYPE_CREATE);
	}

	@Override
	public void assertShiftTypeUpdateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_TYPE_UPDATE);
	}

	@Override
	public void assertShiftTypeGetListAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_TYPE_GET_LIST);
	}

	@Override
	public void assertShiftTypeDeleteAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_TYPE_DELETE);
	}

	@Override
	public void assertWorkShiftCreateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_WORK_SHIFT_CREATE);
	}

	@Override
	public void assertWorkShiftUpdateAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_WORK_SHIFT_UPDATE);
	}

	@Override
	public void assertWorkShiftDeleteAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_WORK_SHIFT_DELETE);
	}

	@Override
	public void assertWorkShiftGetWeekdayAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_WORK_SHIFT_GET_WEEKDAY);
	}

	@Override
	public void assertWorkShiftGetListAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_WORK_SHIFT_GET_LIST);
	}

	@Override
	public void assertShiftDefaultGetListAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_DEFAULT_GET_LIST);
	}

	@Override
	public void assertShiftDefaultDeleteAllowed(Map<String, Object> operatorJwtUser) {
		assertRouteAllowed(operatorJwtUser, ROUTE_ALIAS_SHIFT_DEFAULT_DELETE);
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
		throw new ForbiddenException("您没有操作权限【" + routeAlias + "】");
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
