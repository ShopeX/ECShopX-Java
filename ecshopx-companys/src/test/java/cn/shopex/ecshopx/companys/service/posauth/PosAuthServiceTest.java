package cn.shopex.ecshopx.companys.service.posauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import cn.shopex.ecshopx.common.auth.PosJwtIssuerPort;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
class PosAuthServiceTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	@Mock
	private PosJwtIssuerPort posJwtIssuerPort;

	private PosAuthService service;
	private SensitiveFieldEncryptor encryptor;
	private StaticMessageSource messageSource;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PosStoreAccount.class);
	}

	@BeforeEach
	void setUp() {
		encryptor = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		messageSource = new StaticMessageSource();
		messageSource.addMessage(PosAuthMessages.BAD_CREDENTIALS, Locale.SIMPLIFIED_CHINESE, "账号或密码错误");
		messageSource.addMessage(PosAuthMessages.ACCOUNT_DISABLED, Locale.SIMPLIFIED_CHINESE, "账号已禁用");
		messageSource.addMessage(PosAuthMessages.ACCOUNT_RESIGNED, Locale.SIMPLIFIED_CHINESE, "账号已离职");
		messageSource.addMessage(
				PosAuthMessages.ACCOUNT_DATA_ABNORMAL, Locale.SIMPLIFIED_CHINESE, "账号数据异常，请联系管理员");
		messageSource.addMessage(PosAuthMessages.SHOP_CLOSED, Locale.SIMPLIFIED_CHINESE, "门店已闭店，无法登录");
		messageSource.addMessage(PosAuthMessages.SHOP_DELETED, Locale.SIMPLIFIED_CHINESE, "门店已撤店，无法登录");
		messageSource.addMessage(PosAuthMessages.SHOP_MISSING, Locale.SIMPLIFIED_CHINESE, "门店不存在，无法登录");
		messageSource.addMessage(
				PosAuthMessages.BAD_CREDENTIALS, Locale.US, "Incorrect account or password");
		LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
		service = new PosAuthService(mapper, shopMapper, encryptor, posJwtIssuerPort, messageSource);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void loginSuccessIssuesPosToken() {
		PosStoreAccount row = enabledRow("A01", "secret");
		when(mapper.selectList(any())).thenReturn(List.of(row));
		when(shopMapper.selectShopByCompanyAndId(1L, 88L)).thenReturn(shop("true"));
		when(posJwtIssuerPort.issueToken(anyMap())).thenReturn("pos.jwt.token");
		Map<String, Object> out = service.login(" A01 ", "secret");
		assertEquals("pos.jwt.token", out.get("token"));
		assertEquals(9L, out.get("id"));
		assertEquals("A01", out.get("employee_no"));
		verify(posJwtIssuerPort)
				.issueToken(
						argThat(
								claims ->
										"A01".equals(claims.get("employee_no"))
												&& "A01".equals(claims.get("employee_id"))));
	}

	@Test
	void loginSuccessWhenShopDisabled() {
		PosStoreAccount row = enabledRow("A01", "secret");
		when(mapper.selectList(any())).thenReturn(List.of(row));
		when(shopMapper.selectShopByCompanyAndId(1L, 88L)).thenReturn(shop("false"));
		when(posJwtIssuerPort.issueToken(anyMap())).thenReturn("pos.jwt.token");
		Map<String, Object> out = service.login("A01", "secret");
		assertEquals("pos.jwt.token", out.get("token"));
	}

	@Test
	void wrongPasswordIsFuzzy() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret")));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "bad"));
		assertEquals("账号或密码错误", ex.getMessage());
	}

	@Test
	void wrongPasswordUsesLocaleMessage() {
		LocaleContextHolder.setLocale(Locale.US);
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret")));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "bad"));
		assertEquals("Incorrect account or password", ex.getMessage());
	}

	@Test
	void missingAccountIsFuzzy() {
		when(mapper.selectList(any())).thenReturn(List.of());
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("账号或密码错误", ex.getMessage());
	}

	@Test
	void disabledRejected() {
		PosStoreAccount row = enabledRow("A01", "secret");
		row.setStatus("disabled");
		when(mapper.selectList(any())).thenReturn(List.of(row));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("账号已禁用", ex.getMessage());
	}

	@Test
	void resignedRejected() {
		PosStoreAccount row = enabledRow("A01", "secret");
		row.setStatus("resigned");
		when(mapper.selectList(any())).thenReturn(List.of(row));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("账号已离职", ex.getMessage());
	}

	@Test
	void closedShopRejected() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret")));
		when(shopMapper.selectShopByCompanyAndId(1L, 88L)).thenReturn(shop("closed"));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("门店已闭店，无法登录", ex.getMessage());
	}

	@Test
	void deletedShopRejected() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret")));
		when(shopMapper.selectShopByCompanyAndId(1L, 88L)).thenReturn(shop("delete"));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("门店已撤店，无法登录", ex.getMessage());
	}

	@Test
	void missingShopRejected() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret")));
		when(shopMapper.selectShopByCompanyAndId(1L, 88L)).thenReturn(null);
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertEquals("门店不存在，无法登录", ex.getMessage());
	}

	@Test
	void ambiguousRowsRejected() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledRow("A01", "secret"), enabledRow("A01", "x")));
		UnauthorizedException ex =
				assertThrows(UnauthorizedException.class, () -> service.login("A01", "secret"));
		assertTrue(ex.getMessage().contains("账号数据异常"));
	}

	private static Map<String, Object> shop(String isValid) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", 88L);
		m.put("name", "测试店");
		m.put("shop_code", "S88");
		m.put("is_valid", isValid);
		return m;
	}

	private PosStoreAccount enabledRow(String employeeNo, String plain) {
		PosStoreAccount row = new PosStoreAccount();
		row.setId(9L);
		row.setEmployeeNo(employeeNo);
		row.setName("张三");
		row.setRole("clerk");
		row.setCompanyId(1L);
		row.setDistributorId(88L);
		row.setStatus("enabled");
		row.setPasswordCipher(encryptor.encryptAlways(plain));
		return row;
	}
}
