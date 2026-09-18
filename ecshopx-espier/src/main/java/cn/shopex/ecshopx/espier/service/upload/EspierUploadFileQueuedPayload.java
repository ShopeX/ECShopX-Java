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

package cn.shopex.ecshopx.espier.service.upload;

import java.util.Map;

public final class EspierUploadFileQueuedPayload {

	private final long id;
	private final long companyId;
	private final long operatorId;
	private final long supplierId;
	private final long distributorId;
	private final long merchantId;
	private final long relationId;
	private final String fileType;
	private final String storagePath;
	private final String requestFileType;
	private final String operatorType;

	private EspierUploadFileQueuedPayload(
			long id,
			long companyId,
			long operatorId,
			long supplierId,
			long distributorId,
			long merchantId,
			long relationId,
			String fileType,
			String storagePath,
			String requestFileType,
			String operatorType) {
		this.id = id;
		this.companyId = companyId;
		this.operatorId = operatorId;
		this.supplierId = supplierId;
		this.distributorId = distributorId;
		this.merchantId = merchantId;
		this.relationId = relationId;
		this.fileType = fileType;
		this.storagePath = storagePath;
		this.requestFileType = requestFileType;
		this.operatorType = operatorType;
	}

	public static EspierUploadFileQueuedPayload fromPersistedRow(
			Map<String, Object> result, String storagePath, String requestFileType) {
		return new EspierUploadFileQueuedPayload(
				toLong(result.get("id")),
				toLong(result.get("company_id")),
				toLong(result.get("operator_id")),
				toLong(result.get("supplier_id")),
				toLong(result.get("distributor_id")),
				toLong(result.get("merchant_id")),
				toLong(result.get("relation_id")),
				String.valueOf(result.get("file_type")),
				storagePath,
				requestFileType,
				operatorTypeFromRow(result));
	}

	private static String operatorTypeFromRow(Map<String, Object> result) {
		Object o = result.get("operator_type");
		if (o == null) {
			return "";
		}
		String s = String.valueOf(o).trim();
		return s;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	public long getId() {
		return id;
	}

	public long getCompanyId() {
		return companyId;
	}

	public long getOperatorId() {
		return operatorId;
	}

	public long getSupplierId() {
		return supplierId;
	}

	public long getDistributorId() {
		return distributorId;
	}

	public long getMerchantId() {
		return merchantId;
	}

	public long getRelationId() {
		return relationId;
	}

	public String getFileType() {
		return fileType;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public String getRequestFileType() {
		return requestFileType;
	}

	public String getOperatorType() {
		return operatorType;
	}
}
