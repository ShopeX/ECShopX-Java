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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileJobEnqueuePort;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EspierUploadFileJobDispatchPublisherImpl implements EspierUploadFileJobEnqueuePort {

	private final DispatchFacade dispatchFacade;

	public EspierUploadFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishAfterCommit(EspierUploadFileQueuedPayload payload) {
		Map<String, Object> map = toPayloadMap(payload);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			dispatchAfterCommit(map);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				dispatchAfterCommit(map);
			}
		});
	}

	private void dispatchAfterCommit(Map<String, Object> map) {
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.UPLOAD_FILE_JOB,
				map,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	private static Map<String, Object> toPayloadMap(EspierUploadFileQueuedPayload payload) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", payload.getId());
		m.put("company_id", payload.getCompanyId());
		m.put("operator_id", payload.getOperatorId());
		m.put("supplier_id", payload.getSupplierId());
		m.put("distributor_id", payload.getDistributorId());
		m.put("merchant_id", payload.getMerchantId());
		m.put("relation_id", payload.getRelationId());
		m.put("file_type", payload.getFileType());
		m.put("storage_path", payload.getStoragePath());
		m.put("request_file_type", payload.getRequestFileType());
		m.put("operator_type", payload.getOperatorType());
		return m;
	}
}
