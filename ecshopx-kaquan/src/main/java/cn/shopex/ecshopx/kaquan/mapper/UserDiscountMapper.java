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

package cn.shopex.ecshopx.kaquan.mapper;

import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.service.discount.dto.OpenapiUserDiscountListFilterParams;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountListFilterParams;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountMyCardListFilterParams;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDiscountMapper extends BaseMapper<UserDiscount> {

	@Select("<script>"
			+ "SELECT card_id AS cardId, COUNT(*) AS num FROM kaquan_user_discount "
			+ "WHERE company_id = #{companyId} AND card_id IN "
			+ "<foreach collection=\"cardIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
			+ "#{id}"
			+ "</foreach> "
			+ "GROUP BY card_id"
			+ "</script>")
	List<UserDiscountCardAggRow> countReceivedGroupByCardId(@Param("companyId") long companyId,
			@Param("cardIds") List<Long> cardIds);

	@Select("<script>"
			+ "SELECT card_id AS cardId, COUNT(*) AS num FROM kaquan_user_discount "
			+ "WHERE company_id = #{companyId} AND status = 2 AND card_id IN "
			+ "<foreach collection=\"cardIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
			+ "#{id}"
			+ "</foreach> "
			+ "GROUP BY card_id"
			+ "</script>")
	List<UserDiscountCardAggRow> countVerifiedGroupByCardId(@Param("companyId") long companyId,
			@Param("cardIds") List<Long> cardIds);

	@Select("<script>"
			+ "SELECT card_id AS cardId, COUNT(*) AS num FROM kaquan_user_discount "
			+ "WHERE company_id = #{companyId} AND card_id IN "
			+ "<foreach collection=\"cardIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
			+ "#{id}"
			+ "</foreach> "
			+ "GROUP BY card_id"
			+ "</script>")
	List<UserDiscountCardAggRow> countIssuedGroupByCardId(@Param("companyId") long companyId,
			@Param("cardIds") List<Long> cardIds);

	@Select("<script>"
			+ "SELECT card_id AS cardId, COUNT(*) AS num FROM kaquan_user_discount "
			+ "WHERE company_id = #{companyId} AND user_id = #{userId} AND card_id IN "
			+ "<foreach collection=\"cardIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
			+ "#{id}"
			+ "</foreach> "
			+ "GROUP BY card_id"
			+ "</script>")
	List<UserDiscountCardAggRow> countIssuedGroupByCardIdForUser(@Param("companyId") long companyId,
			@Param("userId") long userId,
			@Param("cardIds") List<Long> cardIds);

	@Select("<script>"
			+ "SELECT card_id AS cardId, COUNT(*) AS num FROM kaquan_user_discount "
			+ "WHERE company_id = #{companyId} AND salesperson_code = #{salespersonCode} AND card_id IN "
			+ "<foreach collection=\"cardIds\" item=\"id\" open=\"(\" separator=\",\" close=\")\">"
			+ "#{id}"
			+ "</foreach> "
			+ "GROUP BY card_id"
			+ "</script>")
	List<UserDiscountCardAggRow> countIssuedGroupByCardIdForSalesperson(@Param("companyId") long companyId,
			@Param("salespersonCode") String salespersonCode,
			@Param("cardIds") List<Long> cardIds);

	long countNewUserCardListDistinct(@Param("f") UserDiscountListFilterParams f);

	List<Map<String, Object>> selectNewUserCardListPage(@Param("f") UserDiscountListFilterParams f, @Param("offset") long offset,
			@Param("limit") int limit);

	long countMyUserCardListDistinct(@Param("f") UserDiscountMyCardListFilterParams f);

	List<Map<String, Object>> selectMyUserCardListPage(
			@Param("f") UserDiscountMyCardListFilterParams f,
			@Param("offset") long offset,
			@Param("limit") int limit);

	List<Long> selectIdsUseBoundAll(@Param("f") UserDiscountListFilterParams f);

	List<Long> selectIdsUseBoundNormal(@Param("f") UserDiscountListFilterParams f);

	List<Long> selectIdsUseBoundNormalNeq(@Param("f") UserDiscountListFilterParams f);

	List<Long> selectIdsUseBoundCategory(@Param("f") UserDiscountListFilterParams f);

	List<Long> selectIdsUseBoundTag(@Param("f") UserDiscountListFilterParams f);

	List<Long> selectIdsUseBoundBrand(@Param("f") UserDiscountListFilterParams f);

	/**
	 * 已过期且仍为兑换锁定的卡券主键，供定时任务批量拉取（analysis §3 步骤 2 / §5 filter）。
	 */
	List<Long> selectExpiredLockedCardIds(@Param("nowEpoch") int nowEpoch, @Param("limit") int limit);

	long countOpenapiUserDiscountListDistinct(@Param("f") OpenapiUserDiscountListFilterParams f);

	List<Map<String, Object>> selectOpenapiUserDiscountListPage(
			@Param("f") OpenapiUserDiscountListFilterParams f,
			@Param("offset") long offset,
			@Param("limit") int limit);
}
