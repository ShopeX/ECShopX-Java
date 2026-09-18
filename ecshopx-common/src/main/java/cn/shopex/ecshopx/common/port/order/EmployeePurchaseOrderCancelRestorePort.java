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

package cn.shopex.ecshopx.common.port.order;

/**
 * 内购订单取消后恢复活动额度与明细累计（orders 模块通过此端口委托 employee-purchase，避免循环依赖）。
 */
public interface EmployeePurchaseOrderCancelRestorePort {

	/** 已支付取消审核通过后恢复（order_class=employee_purchase）。 */
	void restoreAfterRefundPassApproved(long companyId, long orderId);

	/**
	 * 未支付取消成功后恢复额度/活动库存（order_class=employee_purchase）。
	 * 对齐 PHP EmployeePurchase NormalOrderService::cancelOrder 在 refund_status=SUCCESS 时的 restore。
	 */
	void restoreOnOrderCancel(long orderId, long companyId);

	/** 订单关联活动是否共享库存；无关联行时返回 false。 */
	boolean isShareStore(long companyId, long orderId);
}
