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

package cn.shopex.ecshopx.community.service.espier;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommunityChiefEspierImportRowSink implements EspierImportRowSink {

	private final CommunityChiefService communityChiefService;

	public CommunityChiefEspierImportRowSink(CommunityChiefService communityChiefService) {
		this.communityChiefService = communityChiefService;
	}

	@Override
	public String supportedFileType() {
		return "community_chief";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(row);
		if (!merged.containsKey("distributor_id") || merged.get("distributor_id") == null
				|| "0".equals(String.valueOf(merged.get("distributor_id")).trim())) {
			if (distributorId > 0L) {
				merged.put("distributor_id", distributorId);
			}
		}
		communityChiefService.importChiefUploadExcelRow(companyId, distributorId, merged);
	}
}
