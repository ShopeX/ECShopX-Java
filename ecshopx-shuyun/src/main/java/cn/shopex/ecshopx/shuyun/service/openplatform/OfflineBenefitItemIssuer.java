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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;

/**
 * 线下权益明细发券。生产默认 kaquan；联调可配 stub。
 * 对齐 PHP {@code ShuyunOfflineBenefitItemIssuerInterface}。
 */
public interface OfflineBenefitItemIssuer {

	OfflineBenefitIssueResult issue(ShuyunOfflineBenefitSendBatch batch, ShuyunOfflineBenefitSendItem item);
}
