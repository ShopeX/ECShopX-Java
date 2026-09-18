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
 * 查询当前商户下、指定订单已审核（check_status=1）的线下转账记录，用于退款侧「线下银行信息」展示。
 *
 * @param orderId 与调用方约定为 **字符串语义** 传入（调用方保证非 null 且 trim 后非空）。实现内对 **非数字** 字符串须返回空列表，**不得**抛业务校验异常。
 * @return 无记录或非数字 order_id 时返回 {@link java.util.Collections#emptyList()}，JSON 序列化为 {@code []}；
 *         有记录时返回 {@link java.util.LinkedHashMap}{@code <String, Object>}（snake_case 字段，与 admin 线下转账列表单行一致）
 */
public interface RefundOfflineBankReadPort {

	Object getApprovedOfflinePaymentByOrder(long companyId, String orderId);
}
