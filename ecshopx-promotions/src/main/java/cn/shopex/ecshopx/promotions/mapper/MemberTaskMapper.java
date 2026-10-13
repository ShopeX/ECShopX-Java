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

package cn.shopex.ecshopx.promotions.mapper;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberTaskMapper extends BaseMapper<MemberTask> {

	long countAdminList(
			@Param("companyId") long companyId,
			@Param("taskIdKeyword") String taskIdKeyword,
			@Param("nameKeyword") String nameKeyword,
			@Param("typeDetail") String typeDetail,
			@Param("displayStatus") String displayStatus,
			@Param("filterBegin") Integer filterBegin,
			@Param("filterEnd") Integer filterEnd,
			@Param("now") long now);

	List<MemberTask> selectAdminListPage(
			@Param("companyId") long companyId,
			@Param("taskIdKeyword") String taskIdKeyword,
			@Param("nameKeyword") String nameKeyword,
			@Param("typeDetail") String typeDetail,
			@Param("displayStatus") String displayStatus,
			@Param("filterBegin") Integer filterBegin,
			@Param("filterEnd") Integer filterEnd,
			@Param("now") long now,
			@Param("offset") long offset,
			@Param("limit") int limit);

	/** 与列表相同筛选条件，导出概览用（不分页）。 */
	List<MemberTask> selectAdminListForExport(
			@Param("companyId") long companyId,
			@Param("taskIdKeyword") String taskIdKeyword,
			@Param("nameKeyword") String nameKeyword,
			@Param("typeDetail") String typeDetail,
			@Param("displayStatus") String displayStatus,
			@Param("filterBegin") Integer filterBegin,
			@Param("filterEnd") Integer filterEnd,
			@Param("now") long now);
}
