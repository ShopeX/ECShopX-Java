package cn.shopex.ecshopx.mcp;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ArchitectureGuardTest {

	private static final Set<String> FORBIDDEN_BUSINESS_ARTIFACTS = Set.of(
			"ecshopx-orders",
			"ecshopx-goods",
			"ecshopx-companys");

	private static final Pattern ALLOWED_SHOPEX = Pattern.compile("^ecshopx-common$");

	private static final Pattern ALLOWED_ARTIFACT = Pattern.compile(
			"^(ecshopx-common|spring-boot-starter-.+|spring-ai-.+|jjwt-.+|lombok|mockwebserver)$");

	@Test
	void pomDirectDependenciesStayOnArchitectureWhitelist() throws Exception {
		Path pom = resolveModulePom();
		List<DeclaredDependency> dependencies = readDirectDependencies(pom);
		List<String> violations = new ArrayList<>();

		for (DeclaredDependency dependency : dependencies) {
			String artifactId = dependency.artifactId();
			if (FORBIDDEN_BUSINESS_ARTIFACTS.contains(artifactId)) {
				violations.add(dependency + " (forbidden business module)");
				continue;
			}
			if ("cn.shopex".equals(dependency.groupId()) && !ALLOWED_SHOPEX.matcher(artifactId).matches()) {
				violations.add(dependency + " (only ecshopx-common allowed)");
				continue;
			}
			if (!ALLOWED_ARTIFACT.matcher(artifactId).matches()) {
				violations.add(dependency + " (not on dependency whitelist)");
			}
		}

		assertTrue(
				violations.isEmpty(),
				() -> "ecshopx-mcp must not depend on business modules; direct pom dependencies:\n"
						+ String.join("\n", violations));
	}

	private static Path resolveModulePom() {
		Path cwd = Path.of("").toAbsolutePath().normalize();
		Path modulePom = cwd.resolve("pom.xml");
		if (Files.isRegularFile(modulePom)) {
			return modulePom;
		}
		Path nested = cwd.resolve("ecshopx-mcp/pom.xml");
		if (Files.isRegularFile(nested)) {
			return nested;
		}
		fail("Could not locate ecshopx-mcp/pom.xml from " + cwd);
		return modulePom;
	}

	private static List<DeclaredDependency> readDirectDependencies(Path pom) throws Exception {
		try (InputStream in = Files.newInputStream(pom)) {
			Document doc = parseXml(in);
			NodeList nodes = doc.getElementsByTagName("dependency");
			List<DeclaredDependency> result = new ArrayList<>();
			for (int i = 0; i < nodes.getLength(); i++) {
				Element dep = (Element) nodes.item(i);
				if (dep.getParentNode().getNodeName().equals("dependencies")) {
					String groupId = textOf(dep, "groupId");
					String artifactId = textOf(dep, "artifactId");
					if (groupId != null && artifactId != null) {
						result.add(new DeclaredDependency(groupId, artifactId));
					}
				}
			}
			return result;
		}
	}

	private static Document parseXml(InputStream in) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		return factory.newDocumentBuilder().parse(in);
	}

	private static String textOf(Element parent, String tag) {
		NodeList list = parent.getElementsByTagName(tag);
		if (list.getLength() == 0) {
			return null;
		}
		return list.item(0).getTextContent().trim();
	}

	private record DeclaredDependency(String groupId, String artifactId) {

		@Override
		public String toString() {
			return groupId + ":" + artifactId;
		}
	}
}