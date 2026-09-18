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

package cn.shopex.ecshopx.common.port.hfpay;

/**
 * 取现轮询成功终态后的同步事件投递出口；由 dispatch 模块实现并注册至统一 Bus。
 */
public interface HfpayDistributorWithdrawSuccessEventPublishPort {

	/**
	 * 在数据库更新为成功终态后立即发布（同步 listener 同栈消费）。
	 *
	 * @param transAmtFen 提现金额（分），与取现记录行内存字段一致
	 */
	void publishSyncAfterScheduleWithdrawSuccess(
			long hfpayCashRecordId, long companyId, long distributorId, int transAmtFen, String orderId);
}
