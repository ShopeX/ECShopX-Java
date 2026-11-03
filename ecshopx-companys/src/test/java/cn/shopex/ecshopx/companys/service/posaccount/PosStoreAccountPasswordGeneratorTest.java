package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PosStoreAccountPasswordGeneratorTest {

	@Test
	void generateIsEightAlphanumericWithLetterAndDigit() {
		PosStoreAccountPasswordGenerator gen = new PosStoreAccountPasswordGenerator();
		Set<String> samples = new HashSet<>();
		for (int i = 0; i < 80; i++) {
			String p = gen.generate();
			assertEquals(8, p.length());
			assertTrue(p.chars().allMatch(c -> Character.isLetterOrDigit(c)));
			assertTrue(p.chars().anyMatch(Character::isLetter), p);
			assertTrue(p.chars().anyMatch(Character::isDigit), p);
			samples.add(p);
		}
		assertTrue(samples.size() > 1);
	}
}
