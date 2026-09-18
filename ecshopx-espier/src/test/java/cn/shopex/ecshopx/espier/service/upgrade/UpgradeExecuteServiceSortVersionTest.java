package cn.shopex.ecshopx.espier.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpgradeExecuteServiceSortVersionTest {

	@Test
	void sortsMicroSegmentsWithZeroPadThenLexicographicOrder() {
		List<String> in = new ArrayList<>(List.of("1.0.10", "1.0.2", "1.0.0"));
		assertEquals(List.of("1.0.0", "1.0.2", "1.0.10"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void duplicateVersionsRemainStableByOriginalOrder() {
		List<String> in = new ArrayList<>(List.of("1.0.1", "1.0.1"));
		assertEquals(List.of("1.0.1", "1.0.1"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void emptySegmentBetweenDotsNormalizesToZero() {
		List<String> in = new ArrayList<>(List.of("1.0.2", "1..2"));
		assertEquals(List.of("1.0.2", "1.0.2"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void trailingEmptySegmentNormalizesAsTruncatedInt() {
		List<String> in = new ArrayList<>(List.of("1.0.0", "1.0."));
		assertEquals(List.of("1.0.0", "1.0.0"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void leadingPlusAndDigitOnlyFormsCollideAfterIntval() {
		List<String> in = new ArrayList<>(List.of("0.1", "+0.1"));
		assertEquals(List.of("0.1", "0.1"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void signedSecondSegmentSortsByPaddedLexicographicThenIntval() {
		List<String> in = new ArrayList<>(List.of("2.-3", "2.+3"));
		assertEquals(List.of("2.3", "2.-3"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void leadingWhitespaceBeforeDigitsSortsBeforeUnpaddedNumericForm() {
		List<String> in = new ArrayList<>(List.of("10.0", " 10.0"));
		assertEquals(List.of("10.0", "10.0"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void nonNumericSegmentsNormalizeLeadingIntegerToZero() {
		List<String> in = new ArrayList<>(List.of("x.y", "a.b"));
		assertEquals(List.of("0.0", "0.0"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void singleSegmentLeadingZerosCollapseThroughIntval() {
		List<String> in = new ArrayList<>(List.of("010", "10"));
		assertEquals(List.of("10", "10"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void barePlusAndBareMinusSortBeforeDigitsAfterPadding() {
		List<String> in = new ArrayList<>(List.of("9.0", "+", "-"));
		assertEquals(List.of("0", "0", "9.0"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}

	@Test
	void blankVersionStringSortsWithZeroPadThenIntval() {
		List<String> in = new ArrayList<>(List.of("0", ""));
		assertEquals(List.of("0", "0"), UpgradeExecuteService.sortVersionsForUpgrade(in));
	}
}
