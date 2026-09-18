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

package cn.shopex.ecshopx.hfpay.mapper;

import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportContext;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HfpayCashRecordMapper extends BaseMapper<HfpayCashRecord> {

	long countWithdrawExport(@Param("ctx") HfpayWithdrawRecordExportContext ctx);

	List<HfpayWithdrawRecordExportRow> selectWithdrawExportPage(
			@Param("ctx") HfpayWithdrawRecordExportContext ctx,
			@Param("offset") int offset,
			@Param("limit") int limit);

	List<LinkedHashMap<String, Object>> selectWithdrawListPage(
			@Param("ctx") HfpayWithdrawRecordExportContext ctx,
			@Param("offset") int offset,
			@Param("limit") int limit);

	long sumWithdrawTransAmtTotal(@Param("ctx") HfpayWithdrawRecordExportContext ctx);

	long sumWithdrawTransAmtFinish(@Param("ctx") HfpayWithdrawRecordExportContext ctx);

	long sumWithdrawTransAmtInProgress(@Param("ctx") HfpayWithdrawRecordExportContext ctx);

	long sumWithdrawTransAmtFail(@Param("ctx") HfpayWithdrawRecordExportContext ctx);
}
