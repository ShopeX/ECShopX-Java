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

package cn.shopex.ecshopx.goods.service.espier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.goods.service.memberprice.MemberPriceSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalGoodsImportMemberPriceFromRowServiceTest {

	@Mock MemberPriceSaveService memberPriceSaveService;

	NormalGoodsImportMemberPriceFromRowService service;

	@BeforeEach
	void setUp() {
		service = new NormalGoodsImportMemberPriceFromRowService(new ObjectMapper(), memberPriceSaveService);
	}

	@Test
	void saveFromImportRowIfPresent_skipsWhenNoMemberPriceColumns() {
		service.saveFromImportRowIfPresent(1L, 99L, Map.of("price", "10"));
		verify(memberPriceSaveService, never()).saveMemberPrice(eq(1L), eq("99"), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void buildMpriceJson_mergesGradeAndVipColumns() {
		var json =
				service.buildMpriceJson(
						99L,
						Map.of(
								"grade_price4",
								"12.5",
								"vipGrade_price1",
								"0",
								"vipGrade_price2",
								"8"));
		assertThat(json).isPresent();
		assertThat(json.get()).contains("\"99\"");
		assertThat(json.get()).contains("\"grade\"");
		assertThat(json.get()).contains("\"4\":\"12.5\"");
		assertThat(json.get()).contains("\"vipGrade\"");
	}

	@Test
	void saveFromImportRowIfPresent_invokesSaveWhenPricesPresent() {
		service.saveFromImportRowIfPresent(1L, 99L, Map.of("grade_price4", "12.5"));
		verify(memberPriceSaveService).saveMemberPrice(eq(1L), eq("99"), org.mockito.ArgumentMatchers.contains("12.5"));
	}
}
