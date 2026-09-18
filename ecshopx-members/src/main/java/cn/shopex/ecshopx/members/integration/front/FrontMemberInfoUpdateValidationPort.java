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

package cn.shopex.ecshopx.members.integration.front;

import java.util.LinkedHashMap;
import java.util.Map;

/** H5 会员资料更新：配置归一化与 lazy 校验。 */
public interface FrontMemberInfoUpdateValidationPort {

	LinkedHashMap<String, Map<String, Object>> prepareAndValidate(
			long companyId, Map<String, Object> postData, String acceptLanguage);
}
