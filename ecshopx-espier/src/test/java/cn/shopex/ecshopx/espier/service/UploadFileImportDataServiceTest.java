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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.espier.upload.EspierImportBatchSink;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class UploadFileImportDataServiceTest {

	private static final long UPLOAD_ID = 7001L;

	@Mock
	private UploadeFileRepository uploadeFileRepository;

	@Mock
	private EspierUploadFileHandlerRegistry registry;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private EspierUploadTableHandler tableHandler;

	@Mock
	private EspierImportBatchSink batchSink;

	@Test
	void acceptRowsThrows_mergeFinishStateStillRunsAndEachInputRowIsError() throws Exception {
		when(registry.requireHandler("normal_orders")).thenReturn(tableHandler);
		when(batchSink.supportedFileType()).thenReturn("normal_orders");
		when(batchSink.acceptRows(
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyList(),
						anyString()))
				.thenThrow(new RuntimeException("batch accept failed"));

		LinkedHashMap<String, Object> uploadMeta = new LinkedHashMap<>();
		uploadMeta.put("left_job_num", 1);
		uploadMeta.put("handle_message", null);
		uploadMeta.put("relation_id", 0L);
		when(uploadeFileRepository.getInfoById(UPLOAD_ID)).thenReturn(uploadMeta);
		when(uploadeFileRepository.updateOneBy(any(), any())).thenReturn(new LinkedHashMap<>());

		PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

		UploadFileImportDataService service =
				new UploadFileImportDataService(
						uploadeFileRepository,
						new ObjectMapper(),
						registry,
						fileStorageService,
						txManager,
						List.of(batchSink));

		LinkedHashMap<String, Object> uploadInfo = new LinkedHashMap<>();
		uploadInfo.put("id", UPLOAD_ID);
		uploadInfo.put("company_id", 1L);
		uploadInfo.put("operator_id", 2L);
		uploadInfo.put("distributor_id", 0L);
		uploadInfo.put("supplier_id", 0L);
		uploadInfo.put("merchant_id", 0L);
		uploadInfo.put("file_type", "normal_orders");
		uploadInfo.put("operator_type", "admin");
		uploadInfo.put("relation_id", 0L);
		uploadInfo.put("storage_path", "");

		LinkedHashMap<String, Object> row1 = new LinkedHashMap<>();
		row1.put("order_id", "9001");
		row1.put("__excel_row__", 2);
		LinkedHashMap<String, Object> row2 = new LinkedHashMap<>();
		row2.put("order_id", "9002");
		row2.put("__excel_row__", 3);

		service.handleImportData(
				uploadInfo, List.of(row1, row2), Map.of(), 1, List.of("订单号"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> updateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(uploadeFileRepository)
				.updateOneBy(eq(Map.of("id", UPLOAD_ID, "left_job_num", 1)), updateCaptor.capture());

		Map<String, Object> updateFields = updateCaptor.getValue();
		assertThat(updateFields.get("handle_status")).isEqualTo("finish");
		assertThat(updateFields.get("left_job_num")).isEqualTo(0);

		String handleMessageJson = String.valueOf(updateFields.get("handle_message"));
		@SuppressWarnings("unchecked")
		Map<String, Object> handleMessage =
				new ObjectMapper().readValue(handleMessageJson, Map.class);
		assertThat(handleMessage.get("successLine")).isEqualTo(0);
		assertThat(handleMessage.get("errorLine")).isEqualTo(2);

		verify(batchSink)
				.acceptRows(
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyList(),
						anyString());
		verify(uploadeFileRepository).updateOneBy(any(), argThat(m -> m.containsKey("handle_status")));
	}
}
