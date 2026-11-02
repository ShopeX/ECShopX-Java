package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountExportServiceTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	@Mock
	private PosStoreAccountExportDispatchPublisher publisher;

	private PosStoreAccountExportService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PosStoreAccount.class);
	}

	@BeforeEach
	void setUp() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		PosStoreAccountQueryService query = new PosStoreAccountQueryService(mapper, shopMapper);
		PosStoreAccountPasswordService passwords =
				new PosStoreAccountPasswordService(mapper, enc, new PosStoreAccountPasswordGenerator());
		service = new PosStoreAccountExportService(query, passwords, shopMapper, publisher);
	}

	@Test
	void staffCannotExport() {
		Map<String, Object> jwt =
				Map.of("company_id", 1L, "operator_type", "staff", "distributor_ids", List.of(1L));
		ForbiddenException ex =
				assertThrows(ForbiddenException.class, () -> service.enqueue(jwt, Map.of()));
		assertEquals("仅超级管理员可导出账号", ex.getMessage());
	}

	@Test
	void adminEnqueueDoesNotReturnFile() {
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin", "operator_id", 7L);
		Map<String, Object> out = service.enqueue(jwt, Map.of("keyword", "张"));
		assertEquals(true, out.get("status"));
		verify(publisher).enqueue(any());
	}

	@Test
	void emptyFilterStillReturnsNoRows() {
		when(mapper.selectList(any())).thenReturn(List.of());
		List<Map<String, String>> rows =
				service.csvRows(Map.of("company_id", 1L, "operator_type", "admin"));
		assertTrue(rows.isEmpty());
	}

	@Test
	void csvRowsIncludePlainPassword() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		PosStoreAccount row = new PosStoreAccount();
		row.setId(1L);
		row.setCompanyId(1L);
		row.setEmployeeNo("A01");
		row.setName("张三");
		row.setEmail("");
		row.setMobile("");
		row.setRole("clerk");
		row.setDistributorId(88L);
		row.setStatus("enabled");
		row.setPasswordCipher(enc.encryptAlways("Ab12cd34"));
		when(mapper.selectList(any())).thenReturn(List.of(row));
		when(shopMapper.listDistributorNamesByCompanyAndIds(any(Long.class), any()))
				.thenReturn(List.of(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88")));
		List<Map<String, String>> rows =
				service.csvRows(Map.of("company_id", 1L, "operator_type", "admin"));
		assertEquals("Ab12cd34", rows.get(0).get("password"));
		assertEquals("店员", rows.get(0).get("role"));
		assertTrue(!rows.get(0).containsKey("password_cipher"));
	}
}
