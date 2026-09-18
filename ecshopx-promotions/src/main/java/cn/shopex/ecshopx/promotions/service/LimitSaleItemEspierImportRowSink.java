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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Row handler for limit-sale Excel imports. Bulk persistence uses {@link LimitSaleItemUploadService#saveLimitItems}
 * with activity context; per-row {@code handleRow} in the legacy stack is empty, so this validates row shape only.
 */
@Component
public class LimitSaleItemEspierImportRowSink implements EspierImportRowSink {

	@Override
	public String supportedFileType() {
		return "limit_sale_item";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		String shop = str(row.get("shop_code"));
		String bn = str(row.get("item_bn"));
		String lim = str(row.get("limit_num"));
		if (!StringUtils.hasText(shop)) {
			throw new BadRequestException("店铺号必填");
		}
		if (!StringUtils.hasText(bn)) {
			throw new BadRequestException("商品货号必填");
		}
		if (!StringUtils.hasText(lim)) {
			throw new BadRequestException("限购数量必填");
		}
		int n;
		try {
			n = Integer.parseInt(lim.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("限购数量必须是数字");
		}
		if (n <= 0 || n > 9999) {
			throw new BadRequestException("限购数量必须在1-9999之间");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
