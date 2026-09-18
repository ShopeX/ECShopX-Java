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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DistributorWxappShopListFilter {

	private long companyId;
	private String userLng;
	private String userLat;
	private boolean geoEnabled;
	private boolean noHaving;
	private String provinceLikeEscaped;
	private String cityLikeEscaped;
	private String areaLikeEscaped;
	private Integer isZiti;
	private Integer isDelivery;
	private Integer isDada;
	/** When non-null, exact match on {@code distributor_category_id} (C 端：非 null 且非空串，含 0). */
	private Long distributorCategoryIdEq;
	/**
	 * When non-null and non-empty, {@code is_valid IN (...)}. Takes precedence over {@link #isValid}.
	 * PHP 默认 {@code ['true','false']}。
	 */
	private List<String> isValidIn;
	/** When non-null and {@link #isValidIn} empty/null, exact {@code is_valid = ...}. */
	private String isValid;
	private boolean requireShopIdNonEmpty;
	private List<Long> distributorIdInList;
	private List<Long> excludeDistributorIdList;
	private Integer openDivided;
	private Integer searchType;
	private String nameLikeEscaped;
	private List<Long> orDistributorIdsFromItems;
	private boolean useFieldOrder;
	private List<Long> fieldOrderIds;
	private boolean orderByDistanceAsc;
	private boolean orderByDistanceDesc;
	private boolean orderByIsDefaultBranch;
	private boolean orderByCreatedDescOnly;
	private long offset;
	private int limit;

	public DistributorWxappShopListFilter copy() {
		DistributorWxappShopListFilter n = new DistributorWxappShopListFilter();
		n.companyId = this.companyId;
		n.userLng = this.userLng;
		n.userLat = this.userLat;
		n.geoEnabled = this.geoEnabled;
		n.noHaving = this.noHaving;
		n.provinceLikeEscaped = this.provinceLikeEscaped;
		n.cityLikeEscaped = this.cityLikeEscaped;
		n.areaLikeEscaped = this.areaLikeEscaped;
		n.isZiti = this.isZiti;
		n.isDelivery = this.isDelivery;
		n.isDada = this.isDada;
		n.distributorCategoryIdEq = this.distributorCategoryIdEq;
		n.isValidIn = this.isValidIn == null ? null : new ArrayList<>(this.isValidIn);
		n.isValid = this.isValid;
		n.requireShopIdNonEmpty = this.requireShopIdNonEmpty;
		n.distributorIdInList = this.distributorIdInList == null ? null : new ArrayList<>(this.distributorIdInList);
		n.excludeDistributorIdList =
				this.excludeDistributorIdList == null ? null : new ArrayList<>(this.excludeDistributorIdList);
		n.openDivided = this.openDivided;
		n.searchType = this.searchType;
		n.nameLikeEscaped = this.nameLikeEscaped;
		n.orDistributorIdsFromItems =
				this.orDistributorIdsFromItems == null ? null : new ArrayList<>(this.orDistributorIdsFromItems);
		n.useFieldOrder = this.useFieldOrder;
		n.fieldOrderIds = this.fieldOrderIds == null ? null : new ArrayList<>(this.fieldOrderIds);
		n.orderByDistanceAsc = this.orderByDistanceAsc;
		n.orderByDistanceDesc = this.orderByDistanceDesc;
		n.orderByIsDefaultBranch = this.orderByIsDefaultBranch;
		n.orderByCreatedDescOnly = this.orderByCreatedDescOnly;
		n.offset = this.offset;
		n.limit = this.limit;
		return n;
	}
}
