package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaasErpAcVerifierTest {

	private final SaasErpAcVerifier verifier = new SaasErpAcVerifier();

	@Test
	void matchesKnownVectorBasicParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", "1");
		params.put("b", "2");
		params.put("ac", "25477dfa1c9a3f7a16eda2c102d0e350");
		assertTrue(verifier.matches(params, "tok"));
		assertTrue(params.containsKey("ac"));
	}

	@Test
	void matchesKnownVectorTrimmedConcatenation() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", " 1");
		params.put("b", "2");
		params.put("ac", "25477dfa1c9a3f7a16eda2c102d0e350");
		assertTrue(verifier.matches(params, "tok"));
		assertTrue(params.containsKey("ac"));
	}

	@Test
	void matchesKnownVectorListValueBecomesArray() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", new ArrayList<String>());
		params.put("ac", "928ef029d871d45ee9cf3781a6cfb1aa");
		assertTrue(verifier.matches(params, "tok"));
		assertTrue(params.containsKey("ac"));
	}

	@Test
	void rejectsWrongAc() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", "1");
		params.put("b", "2");
		params.put("ac", "deadbeefdeadbeefdeadbeefdeadbeef");
		assertFalse(verifier.matches(params, "tok"));
		assertTrue(params.containsKey("ac"));
	}

	@Test
	void rejectsMissingAc() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", "1");
		params.put("b", "2");
		assertFalse(verifier.matches(params, "tok"));
	}

	@Test
	void rejectsEmptyAc() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("a", "1");
		params.put("b", "2");
		params.put("ac", "");
		assertFalse(verifier.matches(params, "tok"));
		assertTrue(params.containsKey("ac"));
	}
}
