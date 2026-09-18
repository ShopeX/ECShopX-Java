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

package cn.shopex.ecshopx.common.wechat;

import java.util.Collection;
import java.util.Map;

/**
 * 按导购主键批量加载导购展示数据（供企业微信关联列表等场景装配）。
 */
public interface WorkWechatRelSalespersonBatchPort {

	/**
	 * @param salespersonIds 导购 id 集合；为空时返回空 Map
	 * @return 外层 key 为 salesperson_id；同一 id 多条时后者覆盖前者
	 */
	Map<Long, Map<String, Object>> loadBySalespersonIds(Collection<Long> salespersonIds);
}
