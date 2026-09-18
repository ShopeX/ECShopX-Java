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

package cn.shopex.ecshopx.reservation.port;

import java.util.Map;

/**
 * 商家后台预约与资源位相关路由权限（菜单 API 别名校验）。
 */
public interface ReservationRoutePermissionPort {

	/** 菜单别名 {@code reservation.create}（POST 预约占用资源位）。 */
	void assertReservationCreateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code reservation.get.list}（GET 查看预约记录）。 */
	void assertReservationGetListAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code reservation.get.everydaytime}（GET 获取每天预约时间段）。 */
	void assertReservationGetEveryDayTimePeriodAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code reservation.setting.save}（POST 保存预约详细配置）。 */
	void assertReservationSettingSaveAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code reservation.setting.get}（GET 预约配置详细信息）。 */
	void assertReservationSettingGetAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.add}（POST 新增资源位）。 */
	void assertResourceLevelAddAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.update}（PATCH 更新资源位）。 */
	void assertResourceLevelUpdateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.delete}（DELETE 删除资源位）。 */
	void assertResourceLevelDeleteAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.set.status}（PUT 修改资源位状态）。 */
	void assertResourceLevelSetStatusAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.get}（GET 获取资源位详情）。 */
	void assertResourceLevelGetAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code resource.level.list}（GET 获取资源位列表）。 */
	void assertResourceLevelListAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.type.create}（POST 添加排班类型）。 */
	void assertShiftTypeCreateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.type.update}（PATCH 编辑排班类型）。 */
	void assertShiftTypeUpdateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.type.getlist}（GET 排班类型列表）。 */
	void assertShiftTypeGetListAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.type.delete}（DELETE 排班类型）。 */
	void assertShiftTypeDeleteAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code work.shift.create}（POST 新增排班）。 */
	void assertWorkShiftCreateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code work.shift.update}（PATCH /api/v1/workshift）。 */
	void assertWorkShiftUpdateAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code work.shift.delete}（DELETE /api/v1/workshift）。 */
	void assertWorkShiftDeleteAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code work.shift.getweekday}（GET /api/v1/getweekday）。 */
	void assertWorkShiftGetWeekdayAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code work.shift.getlist}（GET /api/v1/workshift 排班列表）。 */
	void assertWorkShiftGetListAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.default.getlist}（GET /api/v1/workshift/default 获取门店默认排班）。 */
	void assertShiftDefaultGetListAllowed(Map<String, Object> operatorJwtUser);

	/** 菜单别名 {@code shift.default.delete}（DELETE /api/v1/workshift/default 删除门店默认排班）。 */
	void assertShiftDefaultDeleteAllowed(Map<String, Object> operatorJwtUser);
}
