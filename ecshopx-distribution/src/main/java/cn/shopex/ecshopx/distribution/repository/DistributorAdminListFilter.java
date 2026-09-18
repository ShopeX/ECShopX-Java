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

package cn.shopex.ecshopx.distribution.repository;

import java.util.List;
import lombok.Data;

@Data
public class DistributorAdminListFilter {

	private Long companyId;
	private List<Long> distributorIdIn;
	private Long merchantIdEq;
	private Boolean unbound;
	private String nameContains;
	private String shopCodeContains;
	private Integer openDivided;
	private String provinceContains;
	private String cityContains;
	private String areaContains;
	private String mobileExact;
	private String merchantNameLike;
	private Integer distributionType;
	private Integer paymentSubject;
	/** When non-null, exact match on {@code distributor_category_id} (admin truthy input). */
	private Long distributorCategoryIdEq;
	private String isValid;
	/**
	 * When non-null and non-empty, {@code is_valid IN (...)} (PHP {@code is_valid=cloud_all}).
	 * Takes precedence over {@link #isValid} when both are set.
	 */
	private List<String> isValidIn;
	/** When null, SQL does not constrain {@code distributor_self} (valid-shop list mode). */
	private Integer distributorSelfEq;
	private Boolean isDistributorEq;
	private String provinceEq;
	private String cityEq;
	private String areaEq;
	private String requestLang;
	private int offset;
	private int limit;

	/** When non-null and {@code 1}, constrains {@code offline_aftersales_other = 1}. */
	private Integer offlineAftersalesOtherEq;
	/** When non-null, {@code distributor_id <> value} (including {@code 0L}). */
	private Long distributorIdNeq;
	/** When non-null and non-empty, {@code distributor_id IN (...)}. */
	private List<Long> distributorIdInConstraint;
	/** When {@code true}, forces empty result set via {@code AND 1 = 0}. */
	private Boolean forceEmptyResult;
	/**
	 * When {@code true}, SQL includes merchant join and {@code merchantName} column; when {@code false},
	 * neither is present. When {@code null}, behaves like {@code true} for backward compatibility.
	 */
	private Boolean includeMerchantJoin;
}
