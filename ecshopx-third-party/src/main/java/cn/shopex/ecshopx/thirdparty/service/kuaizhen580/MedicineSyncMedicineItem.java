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

package cn.shopex.ecshopx.thirdparty.service.kuaizhen580;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 快诊 580「同步药品」单条药品参数（与开放平台 JSON 字段一致）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MedicineSyncMedicineItem(
		@JsonProperty("categoryId") Integer categoryId,
		@JsonProperty("commonName") String commonName,
		@JsonProperty("name") String name,
		@JsonProperty("dosage") String dosage,
		@JsonProperty("spec") String spec,
		@JsonProperty("packingSpec") String packingSpec,
		@JsonProperty("manufacturer") String manufacturer,
		@JsonProperty("approvalNumber") String approvalNumber,
		@JsonProperty("unit") String unit,
		@JsonProperty("medicineId") Long medicineId,
		@JsonProperty("barCode") String barCode,
		@JsonProperty("isPrescription") Integer isPrescription,
		@JsonProperty("price") String price,
		@JsonProperty("stock") String stock,
		@JsonProperty("specialCommonName") String specialCommonName,
		@JsonProperty("specialSpec") String specialSpec) {
}
