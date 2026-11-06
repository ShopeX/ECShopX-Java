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

package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** ECX-10932：签名详情必须限定当前公司，不匹配时返回空 Map。 */
@ExtendWith(MockitoExtension.class)
class AliyunsmsSignInfoServiceCompanyScopeTest {

	@Mock
	private SignMapper signMapper;

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Mock
	private HttpServletRequest request;

	private AliyunsmsSignInfoService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Sign.class);
	}

	@BeforeEach
	void setUp() {
		service = new AliyunsmsSignInfoService(signMapper, namedParameterJdbcTemplate);
	}

	private static Sign sign(long id, Long companyId, String name) {
		Sign sign = new Sign();
		sign.setId(id);
		sign.setCompanyId(companyId);
		sign.setSignName(name);
		sign.setRemark("申请说明");
		sign.setSignFile("sign.png");
		sign.setDelegateFile("delegate.png");
		sign.setQualificationId("q-1");
		sign.setThirdParty(0);
		return sign;
	}

	@Test
	void foreignCompanySign_returnsEmptyMapWithoutLangLookup() {
		lenient().when(signMapper.selectById(77L)).thenReturn(sign(77L, 99L, "他司签名"));
		when(signMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> out = service.getInfo(11L, 77L, request);

		assertThat(out).isEmpty();
		verifyNoInteractions(namedParameterJdbcTemplate);
	}

	@Test
	void signWithoutCompany_returnsEmptyMapWithoutLangLookup() {
		Sign row = sign(77L, 99L, "无公司签名");
		row.setCompanyId(null);
		lenient().when(signMapper.selectById(77L)).thenReturn(row);
		when(signMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> out = service.getInfo(11L, 77L, request);

		assertThat(out).isEmpty();
		verifyNoInteractions(namedParameterJdbcTemplate);
	}

	@Test
	void missingId_returnsEmptyMapWithoutLookup() {
		Map<String, Object> out = service.getInfo(11L, null, request);

		assertThat(out).isEmpty();
		verifyNoInteractions(signMapper);
		verifyNoInteractions(namedParameterJdbcTemplate);
	}

	@Test
	void ownCompanySign_returnsSignName() {
		when(signMapper.selectOne(any())).thenReturn(sign(77L, 11L, "本司签名"));
		when(request.getParameter("country_code")).thenReturn(null);
		when(namedParameterJdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class)))
				.thenReturn(List.of());

		Map<String, Object> out = service.getInfo(11L, 77L, request);

		assertThat(out.get("sign_name")).isEqualTo("本司签名");
		assertThat(out.get("company_id")).isEqualTo(11L);
	}
}
