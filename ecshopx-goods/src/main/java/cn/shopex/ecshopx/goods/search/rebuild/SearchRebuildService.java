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

package cn.shopex.ecshopx.goods.search.rebuild;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexRepository;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchMappingService;
import cn.shopex.ecshopx.goods.search.index.SearchIndexMapper;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSource;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSourceLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 全量重建：新物理索引 → 批量子文档 → alias 切换。 */
@Service
public class SearchRebuildService {

	public static final int BULK_BATCH_SIZE = 500;

	private final GoodsSearchMappingService mappingService;

	private final GoodsSearchIndexRepository indexRepository;

	private final SearchIndexSourceLoader sourceLoader;

	private final SearchIndexMapper indexMapper = new SearchIndexMapper();

	private final LongSupplier epochMillisSupplier;

	@Autowired
	public SearchRebuildService(
			GoodsSearchMappingService mappingService,
			GoodsSearchIndexRepository indexRepository,
			SearchIndexSourceLoader sourceLoader) {
		this(mappingService, indexRepository, sourceLoader, System::currentTimeMillis);
	}

	/** 单测用：可注入固定时钟。 */
	SearchRebuildService(
			GoodsSearchMappingService mappingService,
			GoodsSearchIndexRepository indexRepository,
			SearchIndexSourceLoader sourceLoader,
			LongSupplier epochMillisSupplier) {
		this.mappingService = mappingService;
		this.indexRepository = indexRepository;
		this.sourceLoader = sourceLoader;
		this.epochMillisSupplier = epochMillisSupplier;
	}

	public RebuildResult rebuild(long companyId, Long distributorIdOrNull) throws IOException {
		String physicalIndex = GoodsSearchIndexNames.physicalIndex(epochMillisSupplier.getAsLong());
		mappingService.createIndex(physicalIndex);

		int totalDocs = 0;
		long afterDefaultItemId = 0L;
		long afterDistributorId = 0L;
		while (true) {
			List<SearchIndexSource> batch =
					sourceLoader.fetchPage(
							companyId,
							distributorIdOrNull,
							afterDefaultItemId,
							afterDistributorId,
							BULK_BATCH_SIZE);
			if (batch.isEmpty()) {
				break;
			}
			List<GoodsSearchDocument> documents = new ArrayList<>(batch.size());
			for (SearchIndexSource source : batch) {
				documents.add(indexMapper.toDocument(source));
			}
			indexRepository.bulkIndex(physicalIndex, documents);
			totalDocs += documents.size();
			SearchIndexSource last = batch.get(batch.size() - 1);
			afterDefaultItemId = last.defaultItemId();
			afterDistributorId = last.distributorId();
		}

		indexRepository.switchAlias(physicalIndex);
		return new RebuildResult(physicalIndex, totalDocs);
	}

	public record RebuildResult(String physicalIndex, int docs) {}
}
