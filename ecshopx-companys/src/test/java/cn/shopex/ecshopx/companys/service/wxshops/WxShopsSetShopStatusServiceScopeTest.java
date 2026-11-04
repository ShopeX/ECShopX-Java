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

package cn.shopex.ecshopx.companys.service.wxshops;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxShopsSetShopStatusServiceScopeTest {

	@Mock
	private WxShopsMapper wxShopsMapper;

	@Mock
	private WxShopsJwtShopIdWhitelist whitelist;

	private WxShopsSetShopStatusService service;

	@BeforeEach
	void setUp() {
		service = new WxShopsSetShopStatusService(wxShopsMapper, whitelist);
	}

	@Test
	void setShopStatus_rejectsForeignCompany_beforeUpdate() {
		when(whitelist.allowedShopIds(any())).thenReturn(List.of());
		WxShops foreign = new WxShops();
		foreign.setWxShopId(42L);
		foreign.setCompanyId(200L);
		when(wxShopsMapper.selectById(42L)).thenReturn(foreign);

		assertThatThrownBy(() -> service.setShopStatus(42L, true, Map.<String, Object>of(), 100L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("请确认您的门店信息后再提交.");

		verify(wxShopsMapper, never()).update(isNull(), any(Wrapper.class));
	}

	@Test
	void setShopStatus_updatesOwnCompany_withCompanyScopedWrapper() {
		when(whitelist.allowedShopIds(any())).thenReturn(List.of());
		WxShops own = new WxShops();
		own.setWxShopId(42L);
		own.setCompanyId(100L);
		when(wxShopsMapper.selectById(42L)).thenReturn(own);

		service.setShopStatus(42L, true, Map.<String, Object>of(), 100L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<WxShops>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(wxShopsMapper).update(isNull(), captor.capture());
		String sql = captor.getValue().getSqlSegment();
		// company_id must appear in the WHERE fragment
		org.assertj.core.api.Assertions.assertThat(sql).containsIgnoringCase("company_id");
	}
}
