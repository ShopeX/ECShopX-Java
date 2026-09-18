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

package cn.shopex.ecshopx.common.port.adapay;

/**
 * 自动提现入队；生产侧通过异步消费执行，与 PHP 队列 slow 投递行为对齐。
 */
public interface AdapayDrawCashQueueEnqueuePort {

	void enqueueMainMerchant(long companyId);

	void enqueueSettleAccount(long companyId, long memberId, String settleAccountId);
}
