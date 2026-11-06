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

package cn.shopex.ecshopx.merchant.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import org.junit.jupiter.api.Test;

class MerchantSettlementApplyProgressServiceTest {

	@Test
	void stepChain_toBeSemantics() {
		MerchantSettlementApply empty = new MerchantSettlementApply();
		assertFalse(MerchantSettlementApplyProgressService.isStep1Complete(empty));

		MerchantSettlementApply step1 = new MerchantSettlementApply();
		step1.setSettledType("enterprise");
		step1.setMainCategorySelection("[{\"category_id\":1,\"level\":2}]");
		assertEquals(true, MerchantSettlementApplyProgressService.isStep1Complete(step1));

		MerchantSettlementApply step3 = new MerchantSettlementApply();
		step3.setSettledType("enterprise");
		step3.setMainCategorySelection("[{\"category_id\":1,\"level\":2}]");
		step3.setLicenseUrl("http://license");
		step3.setDisabled(true);
		assertEquals(true, step3.isDisabled());

		MerchantSettlementApply step4 = new MerchantSettlementApply();
		step4.setDisabled(false);
		assertEquals(false, step4.isDisabled());
	}
}
