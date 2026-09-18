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

package cn.shopex.ecshopx.common.cron.youshu;

/**
 * 有数 data source：远端 {@code get} + 条件 {@code add}，对齐 PHP {@code getDataSourcesId} 语义。
 */
public interface YoushuDataSourceApiPort {

	/**
	 * @param dataSourceType PHP 中多为 0；与 PHP 一致参与业务分支命名，实际 HTTP 路径以网关为准。
	 */
	String getOrCreateDataSourceId(String merchantId, int dataSourceType, YoushuOpenApiCredentials credentials);
}
