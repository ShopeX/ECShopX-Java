package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SaasErpCompanyResolverTest {

	private static final long SYSTEM_COMPANY_ID = 9001L;

	private StringRedisTemplate prismRedis;
	private ValueOperations<String, String> prismOps;
	private CompanyPassportUidByCompanyIdPort passportPort;
	private SaasErpCompanyResolver resolver;

	@BeforeEach
	void setUp() {
		prismRedis = Mockito.mock(StringRedisTemplate.class);
		prismOps = Mockito.mock(ValueOperations.class);
		when(prismRedis.opsForValue()).thenReturn(prismOps);
		passportPort = companyId -> Optional.of("passport-" + companyId);
		resolver =
				new SaasErpCompanyResolver(
						passportPort, prismRedis, new ObjectMapper(), SYSTEM_COMPANY_ID);
	}

	@Test
	void nodeHitReturnsCompanyIdAndCompanyKeyToken() {
		String toNodeId = "erp-node-hit";
		when(prismOps.get(nodeRedisKey(toNodeId)))
				.thenReturn("{\"cert_id\":\"nc\",\"node_id\":\"n1\",\"token\":\"node-tok\",\"company_id\":42}");
		when(prismOps.get(certRedisKey("passport-42", 42L)))
				.thenReturn("{\"cert_id\":\"c1\",\"node_id\":\"\",\"token\":\"company-token\"}");

		SaasErpCompanyResolver.Resolved resolved = resolver.resolve(toNodeId);

		assertEquals(42L, resolved.companyId());
		assertEquals("company-token", resolved.token());
	}

	@Test
	void blankNodeUsesSystemCompanyId() {
		when(prismOps.get(certRedisKey("passport-" + SYSTEM_COMPANY_ID, SYSTEM_COMPANY_ID)))
				.thenReturn("{\"cert_id\":\"c1\",\"token\":\"sys-tok\"}");

		SaasErpCompanyResolver.Resolved resolved = resolver.resolve("   ");

		assertEquals(SYSTEM_COMPANY_ID, resolved.companyId());
		assertEquals("sys-tok", resolved.token());
	}

	@Test
	void missingCertificateYieldsEmptyToken() {
		when(prismOps.get(anyString())).thenReturn(null);

		SaasErpCompanyResolver.Resolved resolved = resolver.resolve("");

		assertEquals(SYSTEM_COMPANY_ID, resolved.companyId());
		assertEquals("", resolved.token());
	}

	@Test
	void companyCertWithNodeIdWritesBackToNodeKey() throws Exception {
		long companyId = SYSTEM_COMPANY_ID;
		String mirrorNodeId = "mirror-node-1";
		when(prismOps.get(certRedisKey("passport-" + companyId, companyId)))
				.thenReturn(
						"{\"cert_id\":\"cert-9\",\"node_id\":\""
								+ mirrorNodeId
								+ "\",\"token\":\"write-tok\"}");

		resolver.resolve("");

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(prismOps).set(keyCaptor.capture(), jsonCaptor.capture());
		assertEquals(nodeRedisKey(mirrorNodeId), keyCaptor.getValue());

		ObjectMapper mapper = new ObjectMapper();
		var written = mapper.readTree(jsonCaptor.getValue());
		assertEquals("cert-9", written.get("cert_id").asText());
		assertEquals(mirrorNodeId, written.get("node_id").asText());
		assertEquals("write-tok", written.get("token").asText());
		assertEquals(companyId, written.get("company_id").asLong());
	}

	@Test
	void missingPassportUidUsesEmptyStringForCompanyKey() {
		CompanyPassportUidByCompanyIdPort emptyPassport = companyId -> Optional.empty();
		resolver =
				new SaasErpCompanyResolver(
						emptyPassport, prismRedis, new ObjectMapper(), SYSTEM_COMPANY_ID);
		when(prismOps.get(certRedisKey("", SYSTEM_COMPANY_ID)))
				.thenReturn("{\"cert_id\":\"c1\",\"token\":\"empty-uid-tok\"}");

		SaasErpCompanyResolver.Resolved resolved = resolver.resolve("");

		assertEquals("empty-uid-tok", resolved.token());
		verify(prismOps).get(eq(certRedisKey("", SYSTEM_COMPANY_ID)));
	}

	private static String certRedisKey(String passportUid, long companyId) {
		return "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
	}

	private static String nodeRedisKey(String nodeId) {
		return "prism:" + sha1Hex(nodeId + "_SaasCert");
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
