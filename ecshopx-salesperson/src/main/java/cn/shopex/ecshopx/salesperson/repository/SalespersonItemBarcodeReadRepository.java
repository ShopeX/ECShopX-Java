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

package cn.shopex.ecshopx.salesperson.repository;

import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsBarcodeRow;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonItemsBarcodeReadMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class SalespersonItemBarcodeReadRepository {

	private final SalespersonItemsBarcodeReadMapper mapper;

	public SalespersonItemBarcodeReadRepository(SalespersonItemsBarcodeReadMapper mapper) {
		this.mapper = mapper;
	}

	public SalespersonItemsBarcodeRow findFirstByCompanyIdAndDistributorIdAndBarcode(
			long companyId, long distributorId, String barcode) {
		if (barcode == null) {
			return null;
		}
		LambdaQueryWrapper<SalespersonItemsBarcodeRow> w = new LambdaQueryWrapper<>();
		w.eq(SalespersonItemsBarcodeRow::getCompanyId, companyId)
				.eq(SalespersonItemsBarcodeRow::getDistributorId, distributorId)
				.eq(SalespersonItemsBarcodeRow::getBarcode, barcode)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}
}
