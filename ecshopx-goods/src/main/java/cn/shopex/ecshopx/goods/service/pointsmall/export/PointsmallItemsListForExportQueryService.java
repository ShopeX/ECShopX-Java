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

package cn.shopex.ecshopx.goods.service.pointsmall.export;

import java.util.List;
import java.util.Map;

public interface PointsmallItemsListForExportQueryService {

	/**
	 * item_bn 分支下按与列表接口一致的筛选查询，返回行的 default_item_id 列表（顺序与查询一致）。
	 */
	List<Long> listDefaultItemIdsForExport(Map<String, Object> params);
}
