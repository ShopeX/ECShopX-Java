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

package cn.shopex.ecshopx.goods.search.index;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 占位实现：生产 SQL 物化由 Task 4+ 接入。全量 Job 可调度但不会写入文档，直至真实 loader 替换本 Bean。
 */
@Component
public class StubSearchIndexSourceLoader implements SearchIndexSourceLoader {

	@Override
	public List<SearchIndexSource> fetchPage(
			long companyId, Long distributorIdOrNull, long afterDefaultItemId, int limit) {
		return List.of();
	}
}
