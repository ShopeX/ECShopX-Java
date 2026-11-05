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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.service.reservation.RightsTimesCardRowFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ECX-10881：权益详情须校验归属用户，且在组装详情行之前拒绝。 */
class RightsFrontWxappGetRightsDetailOwnershipTest {

	private RightsMapper rightsMapper;
	private RightsTimesCardRowFactory rightsTimesCardRowFactory;
	private RightsFrontWxappGetRightsDetailService service;

	@BeforeEach
	void setUp() {
		rightsMapper = mock(RightsMapper.class);
		rightsTimesCardRowFactory = mock(RightsTimesCardRowFactory.class);
		service = new RightsFrontWxappGetRightsDetailService(rightsMapper, rightsTimesCardRowFactory);
	}

	@Test
	void getRightsDetail_rejectsOtherUserWithoutBuildingRow() {
		Rights row = new Rights();
		row.setRightsId(12L);
		row.setCompanyId(100L);
		row.setUserId(999L);
		when(rightsMapper.selectById(12L)).thenReturn(row);

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 7L);

		assertThatThrownBy(() -> service.getRightsDetail("12", auth))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("获取权益信息有误，请确认权益 ID");
		verify(rightsTimesCardRowFactory, never()).toWxappRightsDetailRow(any(), anyInt());
	}

	@Test
	void getRightsDetail_returnsOwnRightsDetail() {
		Rights row = new Rights();
		row.setRightsId(12L);
		row.setCompanyId(100L);
		row.setUserId(7L);
		when(rightsMapper.selectById(12L)).thenReturn(row);
		Map<String, Object> built = new LinkedHashMap<>();
		built.put("rights_id", 12L);
		when(rightsTimesCardRowFactory.toWxappRightsDetailRow(any(Rights.class), anyInt())).thenReturn(built);

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 7L);

		Map<String, Object> out = service.getRightsDetail("12", auth);

		assertThat(out.get("rights_id")).isEqualTo(12L);
		assertThat(out).containsKey("server_time");
		verify(rightsTimesCardRowFactory).toWxappRightsDetailRow(any(Rights.class), anyInt());
	}
}
