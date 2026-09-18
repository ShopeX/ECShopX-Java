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

package cn.shopex.ecshopx.espier.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.espier.service.EspierUploadFileAsyncRunner;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EspierUploadFileJobHandler implements DispatchHandler {

	private final EspierUploadFileAsyncRunner espierUploadFileAsyncRunner;

	public EspierUploadFileJobHandler(EspierUploadFileAsyncRunner espierUploadFileAsyncRunner) {
		this.espierUploadFileAsyncRunner = espierUploadFileAsyncRunner;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		espierUploadFileAsyncRunner.runAfterCommit(fromPayloadMap(payload));
	}

	private static EspierUploadFileQueuedPayload fromPayloadMap(Map<String, Object> payload) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", payload.get("id"));
		row.put("company_id", payload.get("company_id"));
		row.put("operator_id", payload.get("operator_id"));
		row.put("supplier_id", payload.get("supplier_id"));
		row.put("distributor_id", payload.get("distributor_id"));
		row.put("merchant_id", payload.get("merchant_id"));
		row.put("relation_id", payload.get("relation_id"));
		row.put("file_type", payload.get("file_type"));
		row.put("operator_type", payload.get("operator_type"));
		String storagePath = payload.get("storage_path") == null ? "" : String.valueOf(payload.get("storage_path"));
		String requestFileType =
				payload.get("request_file_type") == null ? "" : String.valueOf(payload.get("request_file_type"));
		return EspierUploadFileQueuedPayload.fromPersistedRow(row, storagePath, requestFileType);
	}
}
