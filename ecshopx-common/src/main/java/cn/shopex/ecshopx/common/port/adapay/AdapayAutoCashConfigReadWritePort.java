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

import java.util.Map;

/**
 * 自动提现 Redis 配置读写字典；与 {@code draw_limit_config} 系列键的 JSON 存取对齐。
 */
public interface AdapayAutoCashConfigReadWritePort {

	Map<String, Object> getAutoCashConfig(long companyId);

	void putAutoCashConfig(long companyId, Map<String, Object> config);
}
