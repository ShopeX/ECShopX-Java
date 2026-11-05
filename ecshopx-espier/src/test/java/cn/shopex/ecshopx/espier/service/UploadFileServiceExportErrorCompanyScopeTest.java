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

package cn.shopex.ecshopx.espier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.EspierScheduledUploadSourceFileRemover;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileJobEnqueuePort;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileStorageWriter;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10878：上传错误导出必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
class UploadFileServiceExportErrorCompanyScopeTest {

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	@Mock
	private EspierUploadFileStorageWriter storageWriter;

	@Mock
	private EspierUploadFileJobEnqueuePort espierUploadFileJobEnqueuePort;

	@Mock
	private EspierUploadFileAsyncRunner espierUploadFileAsyncRunner;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private EspierScheduledUploadSourceFileRemover scheduledUploadSourceFileRemover;

	@Mock
	private EspierUploadFileHandler handler;

	private UploadFileService service;

	@BeforeEach
	void setUp() {
		service =
				new UploadFileService(
						registry,
						uploadeFileRepository,
						operatorsQueryService,
						storageWriter,
						espierUploadFileJobEnqueuePort,
						espierUploadFileAsyncRunner,
						fileStorageService,
						scheduledUploadSourceFileRemover,
						List.of());
	}

	@Test
	void exportUploadErrorFile_rejectsForeignCompanyWithoutStorageRead() {
		when(registry.requireHandler("goods")).thenReturn(handler);
		when(uploadeFileRepository.getInfoByRouteId("99", 100L)).thenReturn(new LinkedHashMap<>());

		assertThatThrownBy(() -> service.exportUploadErrorFile(100L, "99", "goods"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("上传记录不存在");

		verify(fileStorageService, never()).get(anyString(), anyString());
		verify(fileStorageService, never()).put(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void exportUploadErrorFile_readsStorageForOwnCompanyRecord() {
		when(registry.requireHandler("goods")).thenReturn(handler);
		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		info.put("id", "12");
		info.put("company_id", "100");
		when(uploadeFileRepository.getInfoByRouteId("12", 100L)).thenReturn(info);
		when(fileStorageService.get(eq("file"), eq("uploadFileError/12/error.xlsx")))
				.thenReturn(new byte[] {1, 2, 3});

		Map<String, String> out = service.exportUploadErrorFile(100L, "12", "goods");

		assertThat(out.get("file")).startsWith("data:application/vnd.openxmlformats-officedocument");
		verify(fileStorageService).get("file", "uploadFileError/12/error.xlsx");
		verify(uploadeFileRepository, never()).getInfoByRouteId(eq("12"), eq(999L));
		verify(uploadeFileRepository).getInfoByRouteId("12", 100L);
	}
}
