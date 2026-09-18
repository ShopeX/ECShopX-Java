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

package cn.shopex.ecshopx.common.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ItemsExportCsvPollingPort {

	String getFileName(Map<String, Object> filter);

	/** 表头中文文案列表，顺序与数据行按表头键顺序展开的值一致 */
	List<String> getTitleRow(Map<String, Object> filter);

	int getCount(Map<String, Object> filter);

	/**
	 * 与轮询导出列表一致：第三参为外层进度锚定的 pageSize（100）；实现内以 200 为每页条数查询。
	 */
	List<LinkedHashMap<String, String>> getListsApiReturn(Map<String, Object> filter, int page, int pageSize);
}
