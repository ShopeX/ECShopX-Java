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

package cn.shopex.ecshopx.goods.service.espier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportMemberCardGradesPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalGoodsUploadHeaderTitleServiceTest {

	@Mock AdminMemberExportMemberCardGradesPort memberCardGradesPort;
	@Mock VipGradeListQueryService vipGradeListQueryService;

	NormalGoodsUploadHeaderTitleService service;

	@BeforeEach
	void setUp() {
		service = new NormalGoodsUploadHeaderTitleService(memberCardGradesPort, vipGradeListQueryService);
	}

	@Test
	void forCompany_expandsMemberPriceColumns_andDropsAuditStatus() {
		Map<String, Object> ordinary = new LinkedHashMap<>();
		ordinary.put("grade_id", 4L);
		ordinary.put("grade_name", "普通会员");
		Map<String, Object> vip = new LinkedHashMap<>();
		vip.put("vip_grade_id", 1L);
		vip.put("grade_name", "Pro");
		when(memberCardGradesPort.getGradeListByCompanyId(10L, false)).thenReturn(List.of(ordinary));
		when(vipGradeListQueryService.listVipGradesForWxappMembercardGrades(10L)).thenReturn(List.of(vip));

		UploadHeaderTitle title = service.forCompany(10L);

		assertThat(title.all()).doesNotContainKey("审核状态");
		assertThat(title.all()).doesNotContainKey("会员价");
		assertThat(title.all()).containsEntry("普通会员", "grade_price4");
		assertThat(title.all()).containsEntry("Pro", "vipGrade_price1");
		assertThat(title.all().get("库存")).isEqualTo("store");
	}
}
