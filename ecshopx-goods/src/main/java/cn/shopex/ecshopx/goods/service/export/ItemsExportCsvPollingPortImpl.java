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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.common.export.ItemsExportCsvPollingPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsExportCsvPollingPortImpl implements ItemsExportCsvPollingPort {

	private final GoodsItemsCsvExportService goodsItemsCsvExportService;

	public ItemsExportCsvPollingPortImpl(GoodsItemsCsvExportService goodsItemsCsvExportService) {
		this.goodsItemsCsvExportService = goodsItemsCsvExportService;
	}

	@Override
	public String getFileName(Map<String, Object> filter) {
		return goodsItemsCsvExportService.exportItemsPollingFileName();
	}

	@Override
	public List<String> getTitleRow(Map<String, Object> filter) {
		long companyId = goodsItemsCsvExportService.requireCompanyIdForPolling(filter);
		return goodsItemsCsvExportService.exportItemsPollingTitleValues(companyId);
	}

	@Override
	public int getCount(Map<String, Object> filter) {
		return goodsItemsCsvExportService.exportItemsPollingSkuCount(filter);
	}

	@Override
	public List<LinkedHashMap<String, String>> getListsApiReturn(Map<String, Object> filter, int page, int pageSize) {
		return goodsItemsCsvExportService.exportItemsPollingListRows(filter, page, pageSize);
	}
}
