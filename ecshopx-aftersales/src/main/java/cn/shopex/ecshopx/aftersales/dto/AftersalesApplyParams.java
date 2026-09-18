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

package cn.shopex.ecshopx.aftersales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AftersalesApplyParams {

	private Object detail;
	private List<Map<String, Object>> detailRows;

	@Valid
	@NotEmpty(message = "售后明细商品ID必填")
	private List<ApplyDetailLine> detailLines = new ArrayList<>();

	@NotBlank(message = "售后类型必选")
	private String aftersalesType;

	private Object goodsReturnedRaw;
	private Boolean goodsReturned;

	@NotBlank(message = "售后原因必选")
	private String reason;

	private String description;
	private Object evidencePic;

	private String refundFeeRaw;

	private String refundPointRaw;

	private Integer freight;
	private Long distributorId;

	@NotNull(message = "企业id必填")
	@Min(value = 1, message = "企业id必填")
	private Long companyId;

	private String operatorType;
	private Long operatorId;
	private Long userId;
	private String returnType;
	private Long salesmanId;

	private String contact;
	private String mobile;
	private Long aftersalesAddressId;
	private Long selfDeliveryOperatorId;

	@NotNull(message = "订单号必填,必须为整数")
	@Min(value = 1, message = "订单号必填,必须为整数")
	private Long orderId;

	@AssertTrue(message = "请选择是否到店退货")
	public boolean isGoodsReturnedWhenRefundGoods() {
		if (aftersalesType == null || !"REFUND_GOODS".equalsIgnoreCase(aftersalesType.trim())) {
			return true;
		}
		return goodsReturnedRaw != null;
	}

	@Data
	public static class ApplyDetailLine {

		@NotBlank(message = "售后明细商品ID必填")
		private String id;

		@NotBlank(message = "售后明细商品数量必填")
		private String num;
	}

	public void syncDetailLinesFromRows() {
		List<ApplyDetailLine> lines = new ArrayList<>();
		if (detailRows != null) {
			for (Map<String, Object> row : detailRows) {
				ApplyDetailLine line = new ApplyDetailLine();
				line.setId(row.get("id") == null ? "" : String.valueOf(row.get("id")).trim());
				line.setNum(row.get("num") == null ? "" : String.valueOf(row.get("num")).trim());
				lines.add(line);
			}
		}
		this.detailLines = lines;
	}

	public Map<String, Object> toHandleDataMap() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("order_id", orderId);
		m.put("user_id", userId);
		m.put("aftersales_type", aftersalesType);
		m.put("reason", reason);
		m.put("description", description == null ? "" : description);
		m.put("evidence_pic", evidencePic == null ? List.of() : evidencePic);
		m.put("freight", freight == null ? 0 : freight);
		m.put("operator_type", operatorType);
		m.put("operator_id", operatorId);
		m.put("goods_returned", Boolean.TRUE.equals(goodsReturned));
		m.put("return_type", returnType);
		m.put("distributor_id", distributorId == null ? 0L : distributorId);
		m.put("salesman_id", salesmanId == null ? 0L : salesmanId);
		m.put("contact", contact == null ? "" : contact.trim());
		m.put("mobile", mobile == null ? "" : mobile.trim());
		m.put("aftersales_address_id", aftersalesAddressId == null ? 0L : aftersalesAddressId);
		m.put(
				"self_delivery_operator_id",
				selfDeliveryOperatorId == null ? 0L : selfDeliveryOperatorId);
		m.put("refund_fee", refundFeeRaw == null ? "" : refundFeeRaw.trim());
		m.put("refund_point", refundPointRaw == null ? "" : refundPointRaw.trim());
		return m;
	}

	public Map<String, Object> toCheckApplyDataMap() {
		Map<String, Object> m = toHandleDataMap();
		List<Map<String, Object>> copy = new ArrayList<>();
		if (detailRows != null) {
			for (Map<String, Object> row : detailRows) {
				copy.add(new LinkedHashMap<>(row));
			}
		}
		m.put("detail", copy);
		m.put("is_partial_cancel", false);
		return m;
	}
}
