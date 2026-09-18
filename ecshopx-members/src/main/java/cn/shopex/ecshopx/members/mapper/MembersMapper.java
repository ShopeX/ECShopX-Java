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

package cn.shopex.ecshopx.members.mapper;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.dto.AliyunsmsRunTaskMemberRow;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberListQueryFilter;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberListQueryFilter;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberV2ListQueryFilter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MembersMapper extends BaseMapper<Members> {

	@Select("SELECT grade_id FROM membercard_grade WHERE company_id = #{companyId} AND default_grade = 1 LIMIT 1")
	Long selectDefaultGradeId(@Param("companyId") String companyId);

	@Select(
			"SELECT grade_id FROM membercard_grade WHERE company_id = #{companyId} AND grade_id = #{gradeId} LIMIT 1")
	Long selectGradeIdIfExists(@Param("companyId") String companyId, @Param("gradeId") long gradeId);

	@Select(
			"SELECT grade_id FROM membercard_grade WHERE company_id = #{companyId} AND grade_name = #{gradeName} LIMIT 1")
	Long selectGradeIdByCompanyIdAndGradeName(@Param("companyId") long companyId, @Param("gradeName") String gradeName);

	List<MembersContactByUserIdsLookupService.MemberContactRow> selectMembersWithInfoForUserIds(
			@Param("userIds") List<Long> userIds, @Param("limit") int limit);

	Members selectMemberRowForAdminByCompanyAndUserId(
			@Param("companyId") long companyId, @Param("userId") long userId);

	Members selectMemberRowForAdminByCompanyAndMobileEnc(
			@Param("companyId") long companyId, @Param("mobileEnc") String mobileEnc);

	Members selectMemberRowForAdminByCompanyUserIdAndMobileEnc(
			@Param("companyId") long companyId, @Param("userId") long userId, @Param("mobileEnc") String mobileEnc);

	List<Map<String, Object>> selectMemberListForAdmin(
			Page<Map<String, Object>> page, @Param("f") AdminMemberListQueryFilter f);

	long countMemberListForAdmin(@Param("f") AdminMemberListQueryFilter f);

	List<Map<String, Object>> selectMemberListForOpenapi(
			Page<Map<String, Object>> page, @Param("f") OpenapiMemberListQueryFilter f);

	List<Map<String, Object>> selectMemberListForOpenapiAll(@Param("f") OpenapiMemberListQueryFilter f);

	long countMemberListForOpenapi(@Param("f") OpenapiMemberListQueryFilter f);

	List<MembersContactByUserIdsLookupService.MemberContactRow> selectInviterMobileRowsByUserIds(
			@Param("companyId") long companyId, @Param("userIds") List<Long> userIds);

	List<AliyunsmsRunTaskMemberRow> selectMembersForAliyunsmsRunTask(
			@Param("companyId") long companyId, @Param("userIds") List<Long> userIds);

	List<Map<String, Object>> selectOpenapiMemberBriefRowsByCompanyAndMobileEnc(
			@Param("companyId") long companyId, @Param("mobileEnc") String mobileEnc);

	List<Map<String, Object>> selectOpenapiInviterBriefByUserIds(
			@Param("companyId") long companyId, @Param("userIds") List<Long> userIds);

	List<Map<String, Object>> selectMemberV2ListForOpenapi(
			Page<Map<String, Object>> page, @Param("f") OpenapiMemberV2ListQueryFilter f);

	long countMemberV2ListForOpenapiDistinct(@Param("f") OpenapiMemberV2ListQueryFilter f);
}
