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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsShopRoutePermissionService {

	public static final String ROUTE_ALIAS_SCENE_DISABLE_ITEM = "aliyunsms.scene.disableItem";

	public static final String ROUTE_ALIAS_SCENE_ENABLE_ITEM = "aliyunsms.scene.enableItem";

	public static final String ROUTE_ALIAS_SCENE_ADD_ITEM = "aliyunsms.scene.addItem";

	public static final String ROUTE_ALIAS_SCENE_DELETE_ITEM = "aliyunsms.scene.deleteItem";

	public static final String ROUTE_ALIAS_SCENE_GET_DETAIL = "aliyunsms.scene.getDetail";

	public static final String ROUTE_ALIAS_CONFIG_SET = "aliyunsms.config.set";

	public static final String ROUTE_ALIAS_CONFIG_GET = "aliyunsms.config.get";

	public static final String ROUTE_ALIAS_SIGN_ADD = "aliyunsms.sign.add";

	public static final String ROUTE_ALIAS_SIGN_MODIFY = "aliyunsms.sign.modify";

	public static final String ROUTE_ALIAS_SIGN_GET_INFO = "aliyunsms.sign.getInfo";

	public static final String ROUTE_ALIAS_SIGN_GET_LIST = "aliyunsms.sign.getList";

	public static final String ROUTE_ALIAS_SIGN_DELETE = "aliyunsms.sign.delete";

	public static final String ROUTE_ALIAS_STATUS_SET = "aliyunsms.status.set";

	public static final String ROUTE_ALIAS_STATUS_GET = "aliyunsms.status.get";

	public static final String ROUTE_ALIAS_TASK_ADD = "aliyunsms.task.add";

	public static final String ROUTE_ALIAS_TASK_MODIFY = "aliyunsms.task.modify";

	public static final String ROUTE_ALIAS_TASK_REVOKE = "aliyunsms.task.revoke";

	public static final String ROUTE_ALIAS_TASK_GET_INFO = "aliyunsms.task.info";

	public static final String ROUTE_ALIAS_TASK_GET_LIST = "aliyunsms.task.list";

	public static final String ROUTE_ALIAS_TEMPLATE_ADD = "aliyunsms.tmpl.add";

	public static final String ROUTE_ALIAS_TEMPLATE_MODIFY = "aliyunsms.tmpl.modify";

	public static final String ROUTE_ALIAS_TEMPLATE_GET_INFO = "aliyunsms.tmpl.getInfo";

	public static final String ROUTE_ALIAS_TEMPLATE_GET_LIST = "aliyunsms.tmpl.getList";

	public static final String ROUTE_ALIAS_TEMPLATE_DELETE = "aliyunsms.tmpl.delete";

	public static final String ROUTE_ALIAS_RECORD_GET_LIST = "aliyunsms.record.getList";

	public static final String ROUTE_ALIAS_SCENE_GET_LIST = "aliyunsms.scene.getList";

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

	public AliyunsmsShopRoutePermissionService(
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService) {
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
	}

	public void assertSceneDisableItem(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_DISABLE_ITEM);
	}

	public void assertSceneEnableItem(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_ENABLE_ITEM);
	}

	public void assertSceneAddItem(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_ADD_ITEM);
	}

	public void assertSceneDeleteItem(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_DELETE_ITEM);
	}

	public void assertSceneGetDetail(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_GET_DETAIL);
	}

	public void assertConfigSet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_CONFIG_SET);
	}

	public void assertConfigGet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_CONFIG_GET);
	}

	public void assertSignAdd(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SIGN_ADD);
	}

	public void assertSignModify(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SIGN_MODIFY);
	}

	public void assertSignGetInfo(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SIGN_GET_INFO);
	}

	public void assertSignGetList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SIGN_GET_LIST);
	}

	public void assertSignDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SIGN_DELETE);
	}

	public void assertStatusSet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_STATUS_SET);
	}

	public void assertStatusGet(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_STATUS_GET);
	}

	public void assertTaskAdd(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TASK_ADD);
	}

	public void assertTaskModify(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TASK_MODIFY);
	}

	public void assertTaskRevoke(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TASK_REVOKE);
	}

	public void assertTaskGetInfo(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TASK_GET_INFO);
	}

	public void assertTaskGetList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TASK_GET_LIST);
	}

	public void assertTemplateAdd(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TEMPLATE_ADD);
	}

	public void assertTemplateModify(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TEMPLATE_MODIFY);
	}

	public void assertTemplateGetInfo(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TEMPLATE_GET_INFO);
	}

	public void assertTemplateGetList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TEMPLATE_GET_LIST);
	}

	public void assertTemplateDelete(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_TEMPLATE_DELETE);
	}

	public void assertRecordGetList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_RECORD_GET_LIST);
	}

	public void assertSceneGetList(Map<String, Object> user) {
		assertRouteAllowed(user, ROUTE_ALIAS_SCENE_GET_LIST);
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
