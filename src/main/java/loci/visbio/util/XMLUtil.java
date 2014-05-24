/*
 * #%L
 * VisBio application for visualization of multidimensional biological
 * image data.
 * %%
 * Copyright (C) 2002 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.visbio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * XMLUtil contains useful functions for manipulating DOMs.
 */
public final class XMLUtil {

	// -- Static fields --

	/** Document builder for creating DOMs. */
	protected static DocumentBuilder docBuilder;

	// -- Constructor --

	private XMLUtil() {}

	// -- Utility methods --

	/** Creates a new DOM. */
	public static Document createDocument(final String rootName) {
		if (docBuilder == null) {
			try {
				docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			}
			catch (final ParserConfigurationException exc) {
				exc.printStackTrace();
				return null;
			}
		}
		final Document doc = docBuilder.newDocument();
		if (rootName != null) doc.appendChild(doc.createElement(rootName));
		return doc;
	}

	/** Parses a DOM from the given XML file on disk. */
	public static Document parseXML(final File file) {
		try {
			return parseXML(new FileInputStream(file));
		}
		catch (final FileNotFoundException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/** Parses a DOM from the given XML string. */
	public static Document parseXML(final String xml) {
		return parseXML(new ByteArrayInputStream(xml.getBytes()));
	}

	/** Parses a DOM from the given XML input stream. */
	public static Document parseXML(final InputStream is) {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			return db == null ? null : db.parse(is);
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
		catch (final ParserConfigurationException exc) {
			exc.printStackTrace();
		}
		catch (final SAXException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/** Writes the given DOM to the specified file on disk. */
	public static void writeXML(final File file, final Document doc) {
		try {
			final FileOutputStream out = new FileOutputStream(file);
			writeXML(out, doc);
			out.close();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Writes the given DOM to a string.
	 * 
	 * @return The string to which the DOM was written.
	 */
	public static String writeXML(final Document doc) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		writeXML(os, doc);
		return os.toString();
	}

	/** Writes the given DOM to the specified output stream. */
	public static void writeXML(final OutputStream os, final Document doc) {
		try {
			final TransformerFactory transformFactory =
				TransformerFactory.newInstance();
			final Transformer idTransform = transformFactory.newTransformer();
			final Source input = new DOMSource(doc);
			final Result output = new StreamResult(os);
			idTransform.transform(input, output);
		}
		catch (final TransformerException exc) {
			exc.printStackTrace();
		}
		// append newline to end of output
		try {
			os.write(System.getProperty("line.separator").getBytes());
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Appends a child element with the given name to the specified DOM element.
	 */
	public static Element createChild(final Element el, final String name) {
		final Element child = el.getOwnerDocument().createElement(name);
		el.appendChild(child);
		return child;
	}

	/**
	 * Appends a text node with the given information to the specified DOM
	 * element.
	 */
	public static Text createText(final Element el, final String info) {
		final Text text = el.getOwnerDocument().createTextNode(info);
		el.appendChild(text);
		return text;
	}

	/**
	 * Retrieves the given DOM element's first child element with the specified
	 * name. If name is null, the first child of any type is returned.
	 */
	public static Element getFirstChild(final Element el, final String name) {
		final NodeList nodes = el.getChildNodes();
		final int len = nodes.getLength();
		for (int i = 0; i < len; i++) {
			final Node node = nodes.item(i);
			if (!(node instanceof Element)) continue;
			final Element e = (Element) node;
			if (name == null || e.getTagName().equals(name)) return e;
		}
		return null;
	}

	/**
	 * Retrieves the given DOM element's child elements with the specified name.
	 * If name is null, all children are retrieved.
	 */
	public static Element[] getChildren(final Element el, final String name) {
		final Vector v = new Vector();
		final NodeList nodes = el.getChildNodes();
		final int len = nodes.getLength();
		for (int i = 0; i < len; i++) {
			final Node node = nodes.item(i);
			if (!(node instanceof Element)) continue;
			final Element e = (Element) node;
			if (name == null || e.getTagName().equals(name)) v.add(e);
		}
		final Element[] els = new Element[v.size()];
		v.copyInto(els);
		return els;
	}

	/** Gets the text information associated with the given DOM element. */
	public static String getText(final Element el) {
		final NodeList nodes = el.getChildNodes();
		final int len = nodes.getLength();
		for (int i = 0; i < len; i++) {
			final Node node = nodes.item(i);
			if ("#text".equals(node.getNodeName())) return node.getNodeValue();
		}
		return null;
	}

}
