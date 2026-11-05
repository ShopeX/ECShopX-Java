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

package cn.shopex.ecshopx.salesperson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.salesperson.domain.SalemanCustomerComplaint;
import cn.shopex.ecshopx.salesperson.mapper.SalemanCustomerComplaintMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalemanCustomerComplaintReplyServiceScopeTest {

	@Mock
	private SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;

	private SalemanCustomerComplaintReplyService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), SalemanCustomerComplaint.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new SalemanCustomerComplaintReplyService(
						salemanCustomerComplaintMapper, new ObjectMapper());
	}

	@Test
	void reply_rejectsMissingCompanyId() {
		assertThatThrownBy(
						() ->
								service.replySalemanCustomerComplaints(
										42, "ok", Map.of("operator_id", 7, "username", "op")))
				.isInstanceOf(UnauthorizedException.class);

		verify(salemanCustomerComplaintMapper, never()).selectOne(any(Wrapper.class));
		verify(salemanCustomerComplaintMapper, never()).update(any(), any(Wrapper.class));
		verify(salemanCustomerComplaintMapper, never()).updateById(any(SalemanCustomerComplaint.class));
	}

	@Test
	void reply_rejectsForeignCompanyComplaint_beforeWriting() {
		when(salemanCustomerComplaintMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(
						() ->
								service.replySalemanCustomerComplaints(
										42,
										"ok",
										Map.of(
												"company_id",
												100L,
												"operator_id",
												7,
												"username",
												"op",
												"mobile",
												"138")))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<SalemanCustomerComplaint>> captor =
				ArgumentCaptor.forClass(Wrapper.class);
		verify(salemanCustomerComplaintMapper).selectOne(captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(salemanCustomerComplaintMapper, never()).update(any(), any(Wrapper.class));
		verify(salemanCustomerComplaintMapper, never()).updateById(any(SalemanCustomerComplaint.class));
	}

	@Test
	void reply_allowsOwnCompanyComplaint_andScopesWrite() {
		SalemanCustomerComplaint existing = new SalemanCustomerComplaint();
		existing.setId(42);
		existing.setCompanyId(100);
		existing.setUserId(1);
		existing.setUserName("u");
		existing.setUserMobile("1");
		existing.setSalemanId(2);
		existing.setSalemanName("s");
		existing.setSalemanMobile("2");
		existing.setComplaintsContent("c");
		existing.setReplyStatus(false);
		existing.setCreated(1);
		existing.setUpdated(1);

		SalemanCustomerComplaint refreshed = new SalemanCustomerComplaint();
		refreshed.setId(42);
		refreshed.setCompanyId(100);
		refreshed.setUserId(1);
		refreshed.setUserName("u");
		refreshed.setUserMobile("1");
		refreshed.setSalemanId(2);
		refreshed.setSalemanName("s");
		refreshed.setSalemanMobile("2");
		refreshed.setComplaintsContent("c");
		refreshed.setReplyStatus(true);
		refreshed.setReplyContent("[{\"reply_content\":\"ok\"}]");
		refreshed.setCreated(1);
		refreshed.setUpdated(2);

		when(salemanCustomerComplaintMapper.selectOne(any(Wrapper.class)))
				.thenReturn(existing, refreshed);
		when(salemanCustomerComplaintMapper.update(any(SalemanCustomerComplaint.class), any(Wrapper.class)))
				.thenReturn(1);

		Map<String, Object> out =
				service.replySalemanCustomerComplaints(
						42,
						"ok",
						Map.of(
								"company_id",
								100L,
								"operator_id",
								7,
								"username",
								"op",
								"mobile",
								"138"));

		assertThat(out.get("id")).isEqualTo(42);
		assertThat(out.get("company_id")).isEqualTo(100);
		assertThat(out.get("reply_status")).isEqualTo(1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<SalemanCustomerComplaint>> updateCaptor =
				ArgumentCaptor.forClass(Wrapper.class);
		verify(salemanCustomerComplaintMapper)
				.update(any(SalemanCustomerComplaint.class), updateCaptor.capture());
		assertThat(updateCaptor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
		verify(salemanCustomerComplaintMapper, never()).updateById(any(SalemanCustomerComplaint.class));
	}
}
