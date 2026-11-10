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

package cn.shopex.ecshopx.goods.search.sync;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocumentId;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexRepository;
import cn.shopex.ecshopx.goods.search.index.SearchIndexMapper;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSource;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSourceLoader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 增量写入商品搜索索引（alias 读写）。 */
@Service
public class SearchIndexUpdater {

	private static final Logger log = LoggerFactory.getLogger(SearchIndexUpdater.class);

	static final int MAX_ATTEMPTS = 3;

	private final SearchEsProperties properties;
	private final SearchIndexSourceLoader sourceLoader;
	private final GoodsSearchIndexRepository indexRepository;
	private final SearchIndexMapper indexMapper = new SearchIndexMapper();

	public SearchIndexUpdater(
			SearchEsProperties properties,
			SearchIndexSourceLoader sourceLoader,
			GoodsSearchIndexRepository indexRepository) {
		this.properties = properties;
		this.sourceLoader = sourceLoader;
		this.indexRepository = indexRepository;
	}

	public void upsert(long companyId, long distributorId, long defaultItemId) {
		if (!properties.isEnabled()) {
			return;
		}
		String index = GoodsSearchIndexNames.alias(properties);
		try {
			runWithRetry(
					() -> {
						Optional<SearchIndexSource> srcOpt =
								sourceLoader.fetchOne(companyId, distributorId, defaultItemId);
						if (srcOpt.isEmpty()) {
							indexRepository.delete(index, GoodsSearchDocumentId.of(companyId, distributorId, defaultItemId));
							return;
						}
						GoodsSearchDocument doc = indexMapper.toDocument(srcOpt.get());
						indexRepository.bulkIndex(index, List.of(doc));
					},
					"upsert",
					companyId,
					distributorId,
					defaultItemId);
		} catch (IOException e) {
			log.warn(
					"goods search index upsert failed after {} attempts companyId={} distributorId={} defaultItemId={}",
					MAX_ATTEMPTS,
					companyId,
					distributorId,
					defaultItemId,
					e);
		}
	}

	public void delete(long companyId, long distributorId, long defaultItemId) {
		if (!properties.isEnabled()) {
			return;
		}
		String index = GoodsSearchIndexNames.alias(properties);
		try {
			runWithRetry(
					() -> indexRepository.delete(
							index, GoodsSearchDocumentId.of(companyId, distributorId, defaultItemId)),
					"delete",
					companyId,
					distributorId,
					defaultItemId);
		} catch (IOException e) {
			log.warn(
					"goods search index delete failed after {} attempts companyId={} distributorId={} defaultItemId={}",
					MAX_ATTEMPTS,
					companyId,
					distributorId,
					defaultItemId,
					e);
		}
	}

	@FunctionalInterface
	interface IoRunnable {
		void run() throws IOException;
	}

	private static void runWithRetry(
			IoRunnable action, String op, long companyId, long distributorId, long defaultItemId) throws IOException {
		IOException last = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				action.run();
				return;
			} catch (IOException e) {
				last = e;
				if (attempt < MAX_ATTEMPTS) {
					log.debug(
							"goods search index {} attempt {} failed companyId={} distributorId={} defaultItemId={}",
							op,
							attempt,
							companyId,
							distributorId,
							defaultItemId,
							e);
				}
			}
		}
		throw last;
	}
}
