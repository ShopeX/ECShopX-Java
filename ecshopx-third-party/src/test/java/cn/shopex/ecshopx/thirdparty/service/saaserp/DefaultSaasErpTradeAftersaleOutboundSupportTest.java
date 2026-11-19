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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertErpBindReadService;
import org.junit.jupiter.api.Test;

class DefaultSaasErpTradeAftersaleOutboundSupportTest {

	@Test
	void isSaasErpOutboundEnabled_delegatesToErpBind() {
		SaasCertErpBindReadService bind = mock(SaasCertErpBindReadService.class);
		when(bind.isErpBound(38L)).thenReturn(true);
		when(bind.isErpBound(99L)).thenReturn(false);
		DefaultSaasErpTradeAftersaleOutboundSupport support =
				new DefaultSaasErpTradeAftersaleOutboundSupport(bind);

		assertTrue(support.isSaasErpOutboundEnabled(38L));
		assertFalse(support.isSaasErpOutboundEnabled(99L));
		verify(bind).isErpBound(38L);
		verify(bind).isErpBound(99L);
	}
}
