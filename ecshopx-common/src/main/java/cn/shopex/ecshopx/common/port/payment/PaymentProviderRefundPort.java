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
 * 聚合微信/支付宝/银联等原路退款的出口，供消费端 <code>doRefund</code> 路径调用。定时任务
 * {@code schedule_refund} 不引用本端口。
 */
public interface PaymentProviderRefundPort {

	/**
	 * 打点契约字段；无业务语义，具体路由由实现侧完成。
	 */
	void executeProviderRefundTrigger(long companyId, long refundBn, String payType);
}
