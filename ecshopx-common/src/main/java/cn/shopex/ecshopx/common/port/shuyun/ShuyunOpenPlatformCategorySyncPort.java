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

package cn.shopex.ecshopx.common.port.shuyun;

/** 数云开放平台类目同步出站（D2）。goods 注入；默认 NoOp。 */
public interface ShuyunOpenPlatformCategorySyncPort {

	/** auth 允许时合并派发后同步；companyId/categoryId 非法时 no-op。 */
	void dispatchIfAuthAllows(long companyId, long categoryId);
}
