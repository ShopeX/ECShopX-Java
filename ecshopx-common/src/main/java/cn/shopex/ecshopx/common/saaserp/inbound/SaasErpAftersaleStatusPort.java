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

package cn.shopex.ecshopx.common.saaserp.inbound;

import java.util.List;
import java.util.Map;

/**
 * Implementations throw {@link IllegalStateException} with message {@code 未获取到售后单信息}
 * when the aftersales row is missing. Other failures from review or {@code sendBackConfirm} are
 * swallowed and the method returns the result collected so far, or an empty list.
 */
public interface SaasErpAftersaleStatusPort {

	List<Object> update(Map<String, String> params);
}
