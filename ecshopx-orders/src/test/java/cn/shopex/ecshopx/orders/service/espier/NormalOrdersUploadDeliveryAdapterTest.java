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

package cn.shopex.ecshopx.orders.service.espier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadSepItem;
import cn.shopex.ecshopx.orders.service.espier.NormalOrdersUploadDeliveryPort.UploadShipCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrdersUploadDeliveryAdapterTest {

	@Mock AdminNormalOrderDeliveryCoreService deliveryCore;
	@Mock NormalOrdersEspierDeliveryCorpResolver deliveryCorpResolver;

	NormalOrdersUploadDeliveryAdapter adapter;

	private static final UploadShipCommand COMMAND =
			new UploadShipCommand(1L, 7L, "admin", 0L, 9003L, "顺丰", "YD1");

	@BeforeEach
	void setUp() {
		when(deliveryCorpResolver.resolveDeliveryCorpCode(1L, "顺丰", 0L)).thenReturn("SF");
		adapter =
				new NormalOrdersUploadDeliveryAdapter(
						deliveryCore, deliveryCorpResolver, new ObjectMapper());
	}

	@Test
	void deliverWhole_callsDeliverUploadWholeRemaining_withResolvedCorp() {
		adapter.deliverWhole(COMMAND);

		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(deliveryCore).deliverUploadWholeRemaining(paramsCaptor.capture(), isNull());
		verify(deliveryCore, never()).deliveryNormalPhysical(anyMap(), any(), anyString());

		Map<String, Object> params = paramsCaptor.getValue();
		assertThat(params.get("company_id")).isEqualTo(1L);
		assertThat(params.get("order_id")).isEqualTo(9003L);
		assertThat(params.get("supplier_id")).isEqualTo(0);
		assertThat(params.get("delivery_corp")).isEqualTo("SF");
		assertThat(params.get("delivery_code")).isEqualTo("YD1");
		assertThat(params.get("operator_id")).isEqualTo(7L);
		assertThat(params.get("operator_type")).isEqualTo("admin");
	}

	@Test
	void deliverSep_callsDeliveryNormalPhysical_withSepInfoJson() {
		adapter.deliverSep(COMMAND, List.of(new UploadSepItem(11L, 2)));

		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(deliveryCore).deliveryNormalPhysical(paramsCaptor.capture(), isNull(), eq("normal"));
		verify(deliveryCore, never()).deliverUploadWholeRemaining(anyMap(), any());

		Map<String, Object> params = paramsCaptor.getValue();
		assertThat(params.get("delivery_type")).isEqualTo("sep");
		assertThat(params.get("delivery_corp")).isEqualTo("SF");
		String sepInfo = String.valueOf(params.get("sepInfo"));
		assertThat(sepInfo).contains("\"id\":11");
		assertThat(sepInfo).contains("\"delivery_num\":2");
	}

}
