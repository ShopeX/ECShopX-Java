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

package cn.shopex.ecshopx.workwechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatVerifyDomainFile;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatVerifyDomainFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10888：同名域名校验文件属于其他公司时不得覆盖。 */
class WorkWechatVerifyDomainFileServiceCompanyScopeTest {

	private WorkWechatVerifyDomainFileMapper mapper;
	private WorkWechatVerifyDomainFileService service;

	@BeforeEach
	void setUp() {
		mapper = mock(WorkWechatVerifyDomainFileMapper.class);
		service = new WorkWechatVerifyDomainFileService(mapper);
	}

	@Test
	void saveVerifyInfo_rejectsNameOwnedByAnotherCompany() {
		WorkWechatVerifyDomainFile existing = new WorkWechatVerifyDomainFile();
		existing.setId(7L);
		existing.setCompanyId(200L);
		existing.setName("WW_verify_abc");
		existing.setContents("old");
		when(mapper.selectOne(any())).thenReturn(existing);

		assertThatThrownBy(() -> service.saveVerifyInfo(100L, 9L, "WW_verify_abc", "new"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("验证文件名已被占用");

		verify(mapper, never()).updateById(any(WorkWechatVerifyDomainFile.class));
		verify(mapper, never()).updateById(anyCollection());
		verify(mapper, never()).insert(any(WorkWechatVerifyDomainFile.class));
		assertThat(existing.getCompanyId()).isEqualTo(200L);
		assertThat(existing.getContents()).isEqualTo("old");
	}

	@Test
	void saveVerifyInfo_updatesOwnCompanyRowWithoutChangingCompanyId() {
		WorkWechatVerifyDomainFile existing = new WorkWechatVerifyDomainFile();
		existing.setId(7L);
		existing.setCompanyId(100L);
		existing.setName("WW_verify_abc");
		existing.setContents("old");
		when(mapper.selectOne(any())).thenReturn(existing);
		when(mapper.updateById(any(WorkWechatVerifyDomainFile.class))).thenReturn(1);

		service.saveVerifyInfo(100L, 9L, "WW_verify_abc", "new");

		ArgumentCaptor<WorkWechatVerifyDomainFile> cap = ArgumentCaptor.forClass(WorkWechatVerifyDomainFile.class);
		verify(mapper).updateById(cap.capture());
		assertThat(cap.getValue().getCompanyId()).isEqualTo(100L);
		assertThat(cap.getValue().getContents()).isEqualTo("new");
		assertThat(cap.getValue().getOperatorId()).isEqualTo(9L);
		verify(mapper, never()).insert(any(WorkWechatVerifyDomainFile.class));
	}

	@Test
	void saveVerifyInfo_insertsWhenNameIsNew() {
		when(mapper.selectOne(any())).thenReturn(null);

		service.saveVerifyInfo(100L, 9L, "WW_verify_new", "body");

		ArgumentCaptor<WorkWechatVerifyDomainFile> cap = ArgumentCaptor.forClass(WorkWechatVerifyDomainFile.class);
		verify(mapper).insert(cap.capture());
		assertThat(cap.getValue().getCompanyId()).isEqualTo(100L);
		assertThat(cap.getValue().getName()).isEqualTo("WW_verify_new");
		assertThat(cap.getValue().getContents()).isEqualTo("body");
		verify(mapper, never()).updateById(any(WorkWechatVerifyDomainFile.class));
	}
}
