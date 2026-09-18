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

package cn.shopex.ecshopx.common.port.aftersales;

import java.util.Map;

/** 自动驳回任务侧小程序模板外发；生产走真实通道，test-cron 可替换为 Noop。 */
public interface AftersalesAutoRefuseWxaTemplatePort {

	/** 与「SELLER_REFUSE_BUYER / 商家拒绝」模板语义一致。 */
	void sendSellerRefuseBuyer(Map<String, Object> templateData);
}
