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

package cn.shopex.ecshopx.datacube.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MerchantDataStatisticsMapper {

	Long countAftersales(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	Long sumRefundedFee(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	Long sumAmountPayed(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	/** 与 amount_payed 同口径（PHP 侧为独立列，原查询一致）。 */
	Long sumAmountPointPayed(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	Long countOrders(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	/** 与 countOrders 同口径。 */
	Long countOrderPoint(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	Long countTradesSuccessWindow(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	/** 与 countTradesSuccessWindow 同口径。 */
	Long countTradesPointPayedWindow(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	Long sumGmv(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);

	/** 与 sumGmv 同口径。 */
	Long sumGmvPoint(
			@Param("companyId") long companyId,
			@Param("merchantId") long merchantId,
			@Param("start") long start,
			@Param("end") long end);
}
