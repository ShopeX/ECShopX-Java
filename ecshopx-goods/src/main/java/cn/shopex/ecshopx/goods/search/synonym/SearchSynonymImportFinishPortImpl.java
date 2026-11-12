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

package cn.shopex.ecshopx.goods.search.synonym;

import cn.shopex.ecshopx.common.espier.upload.SearchSynonymImportFinishPort;
import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SearchSynonymImportFinishPortImpl implements SearchSynonymImportFinishPort {

	static final String ES_RELOAD_WARNING_HINT =
			"数据已保存，搜索同义词热更新失败，请稍后重试或通过编辑保存触发刷新";

	private static final Logger log = LoggerFactory.getLogger(SearchSynonymImportFinishPortImpl.class);

	private final SearchSynonymService searchSynonymService;
	private final SearchEsProperties searchEsProperties;

	public SearchSynonymImportFinishPortImpl(
			SearchSynonymService searchSynonymService, SearchEsProperties searchEsProperties) {
		this.searchSynonymService = searchSynonymService;
		this.searchEsProperties = searchEsProperties;
	}

	@Override
	public String onImportFinished(long companyId, int successLine, int errorLine) {
		if (successLine <= 0 || !searchEsProperties.isEnabled()) {
			return null;
		}
		try {
			searchSynonymService.reloadToEs(companyId);
			return null;
		} catch (SearchSynonymEsReloadException e) {
			log.warn(
					"search synonym ES reload failed after import companyId={} successLine={} errorLine={}",
					companyId,
					successLine,
					errorLine,
					e);
			return ES_RELOAD_WARNING_HINT;
		} catch (Exception e) {
			log.warn(
					"search synonym ES reload failed after import companyId={} successLine={} errorLine={}",
					companyId,
					successLine,
					errorLine,
					e);
			return ES_RELOAD_WARNING_HINT;
		}
	}
}
