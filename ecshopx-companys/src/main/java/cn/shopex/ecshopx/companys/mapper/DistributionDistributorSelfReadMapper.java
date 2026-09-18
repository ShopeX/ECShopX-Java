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

package cn.shopex.ecshopx.companys.mapper;

import cn.shopex.ecshopx.companys.dto.DistributorMerchantRow;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 读取 {@code distribution_distributor}（与 distribution 模块同库），避免 companys→distribution 的 Maven 环依赖。
 */
@Mapper
public interface DistributionDistributorSelfReadMapper {

	Map<String, Object> selectSelfStoreRow(@Param("companyId") long companyId);

	List<DistributorMerchantRow> listDistributorRowsByMerchant(
			@Param("merchantId") long merchantId, @Param("companyId") long companyId);

	List<Long> listDistributorIdsByCompanyAndRegionauthId(
			@Param("companyId") long companyId, @Param("regionauthId") long regionauthId);

	List<Map<String, Object>> listDistributorNamesByCompanyAndIds(
			@Param("companyId") long companyId, @Param("distributorIds") List<Long> distributorIds);

	List<Map<String, Object>> listMerchantNamesByCompanyAndIds(
			@Param("companyId") long companyId, @Param("merchantIds") List<Long> merchantIds);
}
