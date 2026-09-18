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

package cn.shopex.ecshopx.espier.cron.support;

import cn.shopex.ecshopx.common.cron.EspierExportHistoryZipFileRemover;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import org.springframework.stereotype.Service;

@Service
public class EspierExportHistoryZipFileRemoverImpl implements EspierExportHistoryZipFileRemover {

	private static final String DISK_KEY = "file";

	private final FileStorageService fileStorageService;

	public EspierExportHistoryZipFileRemoverImpl(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@Override
	public void removeExportHistoryZipObjectKey(String objectKey) {
		fileStorageService.delete(DISK_KEY, objectKey);
	}
}
