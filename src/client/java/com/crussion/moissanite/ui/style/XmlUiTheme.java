package com.crussion.moissanite.ui.style;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XmlUiTheme {
	private static final String RESOURCE_PATH = "assets/moissanite/ui/theme/overlay.xml";
	private static final Map<String, Integer> COLOR_TOKENS = new HashMap<>();
	private static final Map<String, Integer> DIMENSION_TOKENS = new HashMap<>();

	static {
		load();
	}

	private XmlUiTheme() {
	}

	public static int color(String token, int fallback) {
		if (token == null || token.isBlank()) {
			return fallback;
		}
		Integer value = COLOR_TOKENS.get(token);
		return value == null ? fallback : value;
	}

	public static int dimension(String token, int fallback) {
		if (token == null || token.isBlank()) {
			return fallback;
		}
		Integer value = DIMENSION_TOKENS.get(token);
		return value == null ? fallback : value;
	}

	private static void load() {
		try (InputStream stream = XmlUiTheme.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
			if (stream == null) {
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			disableExternalEntities(factory);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Element root = builder.parse(stream).getDocumentElement();
			readTokens(root, "color", COLOR_TOKENS, true);
			readTokens(root, "dimension", DIMENSION_TOKENS, false);
		} catch (Exception ignored) {
		}
	}

	private static void readTokens(Element root, String tagName, Map<String, Integer> target, boolean color) {
		NodeList nodes = root.getElementsByTagName(tagName);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (!(node instanceof Element element)) {
				continue;
			}
			String name = element.getAttribute("name");
			String value = element.getAttribute("value");
			if (name == null || name.isBlank() || value == null || value.isBlank()) {
				continue;
			}
			Integer parsed = color ? parseColor(value) : parseDimension(value);
			if (parsed == null) {
				continue;
			}
			target.put(name.trim(), parsed);
		}
	}

	private static Integer parseColor(String value) {
		String text = value.trim();
		try {
			if (text.startsWith("#")) {
				String hex = text.substring(1);
				if (hex.length() == 6) {
					long rgb = Long.parseLong(hex, 16);
					return (int) (0xFF000000L | rgb);
				}
				if (hex.length() == 8) {
					return (int) Long.parseLong(hex, 16);
				}
				if (hex.length() == 3) {
					int r = Integer.parseInt(hex.substring(0, 1), 16);
					int g = Integer.parseInt(hex.substring(1, 2), 16);
					int b = Integer.parseInt(hex.substring(2, 3), 16);
					r = (r << 4) | r;
					g = (g << 4) | g;
					b = (b << 4) | b;
					return (0xFF << 24) | (r << 16) | (g << 8) | b;
				}
				return null;
			}
			return Integer.decode(text);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static Integer parseDimension(String value) {
		String text = value.trim();
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ignored) {
			try {
				return (int) Math.round(Double.parseDouble(text));
			} catch (NumberFormatException ignoredAgain) {
				return null;
			}
		}
	}

	private static void disableExternalEntities(DocumentBuilderFactory factory) {
		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch (Exception ignored) {
		}
		try {
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		} catch (Exception ignored) {
		}
		try {
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		} catch (Exception ignored) {
		}
		try {
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (Exception ignored) {
		}
	}
}

