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
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface EspierUploadFileHandler {

	String supportedFileType();

	void check(MultipartFile file);

	default Optional<Map<String, Object>> trySyncProcess(
			long companyId, long operatorId, long distributorId, long supplierId, MultipartFile file) {
		return Optional.empty();
	}

	default void handleAfterInsertInRequestThread(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> persistedRow,
			String storageRelativePath,
			String requestFileType) {
		// default no-op
	}
}
