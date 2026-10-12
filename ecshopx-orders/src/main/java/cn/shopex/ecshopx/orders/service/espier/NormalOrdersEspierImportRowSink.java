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

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.common.espier.upload.EspierImportBatchResult;
import cn.shopex.ecshopx.common.espier.upload.EspierImportBatchSink;
import cn.shopex.ecshopx.common.espier.upload.EspierImportRowError;
import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadSegmentService.NormalOrdersUploadImportResult;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadSegmentService.RowFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NormalOrdersEspierImportRowSink implements EspierImportRowSink, EspierImportBatchSink {

	private final NormalOrdersUploadSegmentService normalOrdersUploadSegmentService;

	public NormalOrdersEspierImportRowSink(NormalOrdersUploadSegmentService normalOrdersUploadSegmentService) {
		this.normalOrdersUploadSegmentService = normalOrdersUploadSegmentService;
	}

	@Override
	public String supportedFileType() {
		return "normal_orders";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			String operatorType) {
		throw new IllegalStateException("normal_orders 必须整文件导入");
	}

	@Override
	public EspierImportBatchResult acceptRows(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			List<Map<String, Object>> rows,
			String operatorType) {
		NormalOrdersUploadImportResult result =
				normalOrdersUploadSegmentService.importRows(
						companyId, operatorId, distributorId, supplierId, merchantId, rows, operatorType);
		List<EspierImportRowError> errors = new ArrayList<>();
		for (RowFailure failure : result.failures()) {
			errors.add(new EspierImportRowError(failure.row(), failure.message()));
		}
		return new EspierImportBatchResult(result.successLine(), errors);
	}
}
