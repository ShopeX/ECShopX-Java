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

package cn.shopex.ecshopx.common.hfpay.payment;

import java.util.Map;

/**
 * 汇付支付配置按企业加载；与 {@code HfPayPaymentSettingService#loadForCompany} 同语义，供仅依赖该入口的模块注入，
 * 避免在 common 中依赖 ecshopx-hfpay 具体实现类。
 */
public interface HfPayPaymentSettingLoadPort {

	Map<String, Object> loadForCompany(long companyId);
}
