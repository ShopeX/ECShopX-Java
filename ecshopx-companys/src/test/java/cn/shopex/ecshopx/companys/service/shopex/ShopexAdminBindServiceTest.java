package cn.shopex.ecshopx.companys.service.shopex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.TooManyRequestsException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthService;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthTokenResult;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ShopexAdminBindServiceTest {

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private StringRedisTemplate prismRedisTemplate;

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> prismValueOps;

	@Mock
	private ValueOperations<String, String> companysValueOps;

	@Mock
	private PrismOAuthService prismOAuthService;

	@Mock
	private ShopexSmsClientFacade shopexSmsClientFacade;

	@Mock
	private ShopexOauthCertSyncService shopexOauthCertSyncService;

	private ShopexAdminBindService service;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Operators.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Companys.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new ShopexAdminBindService(
						operatorsMapper,
						companysMapper,
						prismRedisTemplate,
						companysRedisTemplate,
						new ObjectMapper(),
						prismOAuthService,
						shopexSmsClientFacade,
						shopexOauthCertSyncService);
	}

	@Test
	void isOperatorShopexBound_falseWhenBindAccountMissing() {
		Operators op = unboundAdmin();
		op.setPassportUid("u1");
		op.setEid("e1");
		op.setShopexBindAccount("");
		assertThat(service.isOperatorShopexBound(op)).isFalse();
	}

	@Test
	void isOperatorShopexBound_trueWhenDbAndPrismCertPresent() {
		when(prismRedisTemplate.opsForValue()).thenReturn(prismValueOps);
		String passportUid = "u_test";
		long companyId = 100L;
		when(prismValueOps.get(certKey(passportUid, companyId)))
				.thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"t1\"}");

		Operators op = unboundAdmin();
		op.setCompanyId(companyId);
		op.setPassportUid(passportUid);
		op.setEid("e1");
		op.setShopexBindAccount("bound@example.com");
		assertThat(service.isOperatorShopexBound(op)).isTrue();
	}

	@Test
	void getStatus_unboundWhenCertMissing() {
		when(operatorsMapper.selectById(1L)).thenReturn(unboundAdmin());
		Map<String, Object> status = service.getStatusForOperatorId(1L);
		assertThat(status).containsEntry("bound", false);
	}

	@Test
	void bind_409WhenAlreadyBound() {
		when(prismRedisTemplate.opsForValue()).thenReturn(prismValueOps);
		Operators bound = unboundAdmin();
		bound.setPassportUid("u_bound");
		bound.setEid("e1");
		bound.setShopexBindAccount("acc");
		when(prismValueOps.get(certKey("u_bound", 200L)))
				.thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"t1\"}");
		bound.setCompanyId(200L);
		when(operatorsMapper.selectById(1L)).thenReturn(bound);

		assertThatThrownBy(() -> service.bindForAdminOperator(1L, Map.of("username", "u", "password", "p")))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("已绑定 Shopex");
		verify(prismOAuthService, never()).exchangePasswordGrant(anyString(), anyString());
	}

	@Test
	void bind_rejectsWhenPassportUidOwnedByOtherOperator() {
		stubRateLimit(1L);
		when(operatorsMapper.selectById(1L)).thenReturn(unboundAdmin());
		when(prismOAuthService.exchangePasswordGrant("u", "p")).thenReturn(token("pu_conflict"));
		Operators other = new Operators();
		other.setOperatorId(99L);
		other.setPassportUid("pu_conflict");
		when(operatorsMapper.selectOne(any())).thenReturn(other);

		assertThatThrownBy(() -> service.bindForAdminOperator(1L, Map.of("username", "u", "password", "p")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("已被其他管理员账号占用");
		verify(operatorsMapper, never()).update(any(), any());
	}

	@Test
	void bind_requiresUsernameAndPassword() {
		stubRateLimit(1L);
		when(operatorsMapper.selectById(1L)).thenReturn(unboundAdmin());

		assertThatThrownBy(() -> service.bindForAdminOperator(1L, Map.of("username", "", "password", "p")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("请填写 Shopex 账号与密码");
		verify(prismOAuthService, never()).exchangePasswordGrant(anyString(), anyString());
	}

	@Test
	void bind_429WhenRateLimited() {
		stubRateLimit(6L);
		when(operatorsMapper.selectById(1L)).thenReturn(unboundAdmin());

		assertThatThrownBy(() -> service.bindForAdminOperator(1L, Map.of("username", "u", "password", "p")))
				.isInstanceOf(TooManyRequestsException.class)
				.hasMessageContaining("绑定失败次数过多");
		verify(prismOAuthService, never()).exchangePasswordGrant(anyString(), anyString());
	}

	@Test
	void bind_successPersistsPassportAndClearsRateKey() {
		stubRateLimit(1L);
		when(prismRedisTemplate.opsForValue()).thenReturn(prismValueOps);
		Operators unbound = unboundAdmin();
		Operators fresh = unboundAdmin();
		fresh.setPassportUid("pu1");
		fresh.setEid("e1");
		fresh.setShopexBindAccount("user@shopex.test");
		when(operatorsMapper.selectById(1L)).thenReturn(unbound, fresh);
		when(prismOAuthService.exchangePasswordGrant("u", "p")).thenReturn(token("pu1"));
		when(operatorsMapper.selectOne(any())).thenReturn(null);
		when(prismValueOps.get(certKey("pu1", 1L)))
				.thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"n1\",\"token\":\"t1\"}");

		Map<String, Object> result = service.bindForAdminOperator(1L, Map.of("username", "u", "password", "p"));

		assertThat(result).containsEntry("bound", true);
		verify(operatorsMapper).update(eq(null), any());
		verify(companysMapper).update(eq(null), any());
		verify(shopexSmsClientFacade).setAccessToken(eq(1L), eq("pu1"), eq("at"), eq(3600L));
		verify(shopexOauthCertSyncService).syncAfterOauthLogin(1L, "pu1", "at");
		verify(companysRedisTemplate).delete("admin_shopex_bind_failed_times:1");
	}

	private void stubRateLimit(long failedTimes) {
		when(companysRedisTemplate.opsForValue()).thenReturn(companysValueOps);
		when(companysValueOps.increment("admin_shopex_bind_failed_times:1")).thenReturn(failedTimes);
		when(companysRedisTemplate.expire(eq("admin_shopex_bind_failed_times:1"), eq(1800L), eq(TimeUnit.SECONDS)))
				.thenReturn(true);
	}

	private static Operators unboundAdmin() {
		Operators op = new Operators();
		op.setOperatorId(1L);
		op.setOperatorType("admin");
		op.setCompanyId(1L);
		op.setMobile("13800000001");
		return op;
	}

	private static PrismOAuthTokenResult token(String passportUid) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("passport_uid", passportUid);
		data.put("eid", "e1");
		data.put("shopexid", "user@shopex.test");
		return new PrismOAuthTokenResult("at", "rt", 3600L, 7200L, data);
	}

	private static String certKey(String passportUid, long companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest((passportUid + "_" + companyId + "_SaasCert").getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return "prism:" + hex;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
