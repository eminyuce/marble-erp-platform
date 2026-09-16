package com.ozerler.marble.validation;

import com.ozerler.marble.exception.FileValidationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects SVG payloads that can execute script, load external resources, or
 * otherwise escape the image sandbox when rendered.
 */
@Component
public class SvgContentValidator {

    private static final int MAX_SVG_BYTES = 2 * 1024 * 1024;

    private static final Set<String> FORBIDDEN_ELEMENTS = Set.of(
            "script", "foreignobject", "iframe", "embed", "object", "link", "meta"
    );

    public void validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw new FileValidationException("SVG file is empty");
        }
        if (content.length > MAX_SVG_BYTES) {
            throw new FileValidationException("SVG file exceeds the maximum allowed size");
        }

        String raw = new String(content, StandardCharsets.UTF_8);
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("<!entity") || lower.contains("<!doctype")) {
            throw new FileValidationException("SVG files must not contain DTD or entity declarations");
        }
        if (lower.contains("javascript:") || lower.contains("vbscript:")) {
            throw new FileValidationException("SVG files must not contain script URLs");
        }

        Document document = parseSecurely(content);
        Element root = document.getDocumentElement();
        if (root == null || !"svg".equals(root.getLocalName() != null ? root.getLocalName().toLowerCase(Locale.ROOT)
                : root.getTagName().toLowerCase(Locale.ROOT).replaceFirst("^.*:", ""))) {
            throw new FileValidationException("File is not a valid SVG document");
        }
        walk(root);
    }

    private Document parseSecurely(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        } catch (FileValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new FileValidationException("SVG file is not well-formed XML");
        }
    }

    private void walk(Element element) {
        String localName = localName(element);
        if (FORBIDDEN_ELEMENTS.contains(localName)) {
            throw new FileValidationException("SVG file contains a forbidden element: " + localName);
        }
        if ("use".equals(localName) || "image".equals(localName)) {
            rejectExternalReference(element, "href");
            rejectExternalReference(element, "xlink:href");
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getName().toLowerCase(Locale.ROOT);
            String attrValue = attr.getValue() != null ? attr.getValue().trim().toLowerCase(Locale.ROOT) : "";
            if (attrName.startsWith("on")) {
                throw new FileValidationException("SVG file contains event handler attributes");
            }
            if (attrValue.startsWith("javascript:") || attrValue.startsWith("vbscript:")
                    || attrValue.startsWith("data:text/html")) {
                throw new FileValidationException("SVG file contains an unsafe URL");
            }
            if (("href".equals(attrName) || attrName.endsWith(":href"))
                    && (attrValue.startsWith("http://") || attrValue.startsWith("https://")
                    || attrValue.startsWith("//"))) {
                throw new FileValidationException("SVG file must not reference external resources");
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                walk(childElement);
            }
        }
    }

    private void rejectExternalReference(Element element, String attributeName) {
        String value = element.getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            return;
        }
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        if (lowered.startsWith("http://") || lowered.startsWith("https://") || lowered.startsWith("//")
                || lowered.startsWith("javascript:") || lowered.startsWith("data:")) {
            throw new FileValidationException("SVG file must not reference external resources");
        }
    }

    private static String localName(Element element) {
        String name = element.getLocalName();
        if (name == null || name.isBlank()) {
            name = element.getTagName();
        }
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
