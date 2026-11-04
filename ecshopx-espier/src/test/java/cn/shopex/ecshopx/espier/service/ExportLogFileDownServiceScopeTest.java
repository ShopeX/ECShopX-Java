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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.domain.ExportLog;
import cn.shopex.ecshopx.espier.mapper.ExportLogMapper;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.net.URI;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ExportLogFileDownServiceScopeTest {

	@Mock
	private ExportLogMapper exportLogMapper;

	@Mock
	private RestTemplate exportLogDownloadRestTemplate;

	private StorageProperties storageProperties;
	private ExportLogFileDownService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ExportLog.class);
	}

	@BeforeEach
	void setUp() {
		storageProperties = new StorageProperties();
		service =
				new ExportLogFileDownService(
						exportLogMapper, storageProperties, exportLogDownloadRestTemplate);
	}

	@Test
	void fileDown_rejectsForeignOrMissingLog_beforeFetchingContent() {
		when(exportLogMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.fileDown("42", 100L, 7L, "admin"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("资源文件不存在");

		verify(exportLogMapper, never()).selectById(anyLong());
		verify(exportLogDownloadRestTemplate, never()).getForObject(any(URI.class), eq(byte[].class));
	}

	@Test
	void fileDown_allowsOwnCompanyOperator_andReadsHttpFile() {
		ExportLog row = new ExportLog();
		row.setLogId(42L);
		row.setCompanyId(100L);
		row.setOperatorId(7L);
		row.setSupplierId(0L);
		row.setExportType("order");
		row.setFileUrl("https://cdn.example.com/exports/a.csv");
		when(exportLogMapper.selectOne(any(Wrapper.class))).thenReturn(row);
		when(exportLogDownloadRestTemplate.getForObject(any(URI.class), eq(byte[].class)))
				.thenReturn("id,name\n1,a".getBytes());

		ExportLogFileDownResult result = service.fileDown("42", 100L, 7L, "admin");

		assertThat(result.csvData()).isEqualTo("id,name\n1,a");
		assertThat(result.fileName()).endsWith("order.csv");
		verify(exportLogMapper, never()).selectById(anyLong());
	}
}
