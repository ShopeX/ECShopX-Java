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

package cn.shopex.ecshopx.common.port.payment;

/**
 * 汇付待确认支付行（pending）的周期重试；由 adapay 模块实现，供 payment 侧调用以避免模块循环依赖。
 */
public interface AdaPayPaymentConfirmRetryPort {

	/**
	 * 查询待重试的确认行并逐条走确认链路；返回本批查询行数（与 PHP 侧 foreach 前集合大小对位）。
	 */
	int adaPayPaymentConfirmRetry();
}
