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

package cn.shopex.ecshopx.common.port.weixin;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 微信「商家转账」单批次状态查询，对应 v3/transfer/batches/batch-id/{id}。
 */
public interface WechatBatchTransferQueryPort {

	JsonNode queryBalanceOrder(long companyId, WechatMerchantV3ApiMaterial material, String outBatchId);
}
