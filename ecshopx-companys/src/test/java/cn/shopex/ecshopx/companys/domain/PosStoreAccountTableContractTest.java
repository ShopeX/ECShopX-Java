package cn.shopex.ecshopx.companys.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PosStoreAccountTableContractTest {

	@Test
	void uniqueIndexIsGlobalEmployeeNo() {
		MpTable table = PosStoreAccount.class.getAnnotation(MpTable.class);
		assertNotNull(table);
		assertEquals("pos_store_account", table.value());
		MpIndex[] unique = table.uniqueIndexes();
		assertEquals(1, unique.length);
		assertEquals("uk_employee_no", unique[0].name());
		assertArrayEquals(new String[] {"employee_no"}, unique[0].columns());
	}

	@Test
	void localFlywayAltersToUkEmployeeNoWhenPresent() throws Exception {
		Path dir = resolveMigrationDir();
		if (!Files.isDirectory(dir)) {
			return;
		}
		String sql;
		try (Stream<Path> files = Files.list(dir)) {
			sql =
					files.filter(p -> p.getFileName().toString().contains("pos_store_account"))
							.map(
									p -> {
										try {
											return Files.readString(p);
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									})
							.reduce("", (a, b) -> a + "\n" + b);
		}
		if (sql.isBlank()) {
			// 本机增量脚本默认不进 git；实体契约已由 uniqueIndexIsGlobalEmployeeNo 锁定
			return;
		}
		assertTrue(sql.contains("uk_employee_no"), sql);
		assertTrue(sql.contains("utf8mb4_unicode_ci") || sql.contains("DROP INDEX"), sql);
	}

	private static Path resolveMigrationDir() {
		Path fromModule = Path.of("").toAbsolutePath().resolve("../ecshopx-bootstrap/src/main/resources/db/migration");
		if (Files.isDirectory(fromModule)) {
			return fromModule.normalize();
		}
		return Path.of("ecshopx-bootstrap/src/main/resources/db/migration").toAbsolutePath().normalize();
	}
}
