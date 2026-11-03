package cn.shopex.ecshopx.espier.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.auth.PosJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosJwtIssuerTest {

	private static final String SECRET =
			Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

	@Mock
	private PosJwtBlacklistPort blacklist;

	private PosJwtIssuer issuer;

	@BeforeEach
	void setUp() {
		issuer = new PosJwtIssuer(blacklist, new ObjectMapper(), SECRET, 120, "ecshopx-pos");
	}

	@Test
	void issueContainsPosKindAndInvalidateBlacklists() {
		String token =
				issuer.issueToken(
						Map.of(
								"id",
								9L,
								"employee_no",
								"A01",
								"role",
								"clerk",
								"company_id",
								1L,
								"distributor_id",
								88L,
								"status",
								"enabled"));
		Map<String, Object> claims =
				OperatorJwtCompactCodec.verifyAndParse(new ObjectMapper(), token, issuer.signingKey());
		assertTrue(PosJwtRequestAttributes.TOKEN_KIND_POS.equals(String.valueOf(claims.get("token_kind"))));
		assertFalse(claims.get("iss").toString().isBlank());
		issuer.invalidate(token);
		ArgumentCaptor<String> tok = ArgumentCaptor.forClass(String.class);
		verify(blacklist).addToBlacklist(tok.capture(), anyLong());
		assertTrue(tok.getValue().equals(token));
	}

	@Test
	void issueUsesHs512AndBase64UrlLikePhp() throws Exception {
		String token =
				issuer.issueToken(
						Map.of(
								"id",
								9L,
								"employee_no",
								"A01",
								"role",
								"clerk",
								"company_id",
								1L,
								"distributor_id",
								88L,
								"status",
								"enabled"));
		String[] parts = token.split("\\.");
		assertEquals(3, parts.length);
		for (String part : parts) {
			assertFalse(part.contains("+"), "segment must be Base64URL (no +): " + part);
			assertFalse(part.contains("/"), "segment must be Base64URL (no /): " + part);
			assertFalse(part.contains("="), "segment must omit padding: " + part);
		}
		String headerJson =
				new String(
						Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
		assertTrue(headerJson.contains("HS512"), headerJson);
		Map<String, Object> claims =
				OperatorJwtCompactCodec.verifyAndParse(new ObjectMapper(), token, issuer.signingKey());
		assertEquals("A01", String.valueOf(claims.get("employee_no")));
	}
}
