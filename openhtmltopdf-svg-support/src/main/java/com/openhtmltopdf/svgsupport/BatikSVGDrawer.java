package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;
import com.openhtmltopdf.util.XRLog;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.Point;
import java.util.List;
import java.util.logging.Level;

public class BatikSVGDrawer implements SVGDrawer {

	private static final String DEFAULT_VP_WIDTH = "400";
	private static final String DEFAULT_VP_HEIGHT = "400";
        private final static Point DEFAULT_DIMENSIONS = new Point(400, 400);
	public OpenHtmlFontResolver fontResolver;
	
	@Override
	public void importFontFaceRules(List<FontFaceRule> fontFaces, SharedContext shared) {
		this.fontResolver = new OpenHtmlFontResolver();
		this.fontResolver.importFontFaces(fontFaces, shared);
	}
	
	@Override
	public void drawSVG(Element svgElement, OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width, double height, double dotsPerPixel) {

		if (this.fontResolver == null) {
			XRLog.general(Level.INFO, "importFontFaceRules has not been called for this pdf transcoder");
			this.fontResolver = new OpenHtmlFontResolver();
		}
		
		PDFTranscoder transcoder = new PDFTranscoder(outputDevice, ctx, x, y, width, height, this.fontResolver, dotsPerPixel);
		
		try {
			DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
			Document newDocument = impl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
			
			for (int i = 0; i < svgElement.getChildNodes().getLength(); i++)
			{
				Node importedNode = newDocument.importNode(svgElement.getChildNodes().item(i), true);
				newDocument.getDocumentElement().appendChild(importedNode);
			}
			
			// Copy attributes such as viewBox to the new SVG document.
			for (int i = 0; i < svgElement.getAttributes().getLength(); i++) {
				Node importedAttr = svgElement.getAttributes().item(i);
				newDocument.getDocumentElement().setAttribute(importedAttr.getNodeName(), importedAttr.getNodeValue());
			}
			
			if (svgElement.hasAttribute("width")) {
				newDocument.getDocumentElement().setAttribute("width", svgElement.getAttribute("width"));
			}
			else {
				newDocument.getDocumentElement().setAttribute("width", DEFAULT_VP_WIDTH);
			}
			
			if (svgElement.hasAttribute("height")) {
				newDocument.getDocumentElement().setAttribute("height", svgElement.getAttribute("height"));
			}
			else {
				newDocument.getDocumentElement().setAttribute("height", DEFAULT_VP_HEIGHT);
			}
			
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float)(width / dotsPerPixel));
			transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float)(height / dotsPerPixel));

			TranscoderInput in = new TranscoderInput(newDocument);
			transcoder.transcode(in, null);
		} catch (TranscoderException e) {
			XRLog.exception("Couldn't draw SVG.", e);
		}
	}
        
		@Override
		public Integer parseLength(String attrValue) {
			// TODO read length with units and convert to dots.
			// length ::= number (~"em" | ~"ex" | ~"px" | ~"in" | ~"cm" | ~"mm" | ~"pt" | ~"pc")?
			try {
				return Integer.valueOf(attrValue);
			} catch (NumberFormatException e) {
				XRLog.general(Level.WARNING, "Invalid integer passed as dimension for SVG: " + attrValue);
				return null;
			}
		}

	@Override
	public Point parseDimensions(Element e) {
		String widthAttr = e.getAttribute("width");
		Integer width = parseLength(widthAttr);
		
		String heightAttr = e.getAttribute("height");
		Integer height = parseLength(heightAttr);
		
		if (width != null && height != null) {
			return new Point(width, height);
		}

		String viewBoxAttr = e.getAttribute("viewBox");
		String[] splitViewBox = viewBoxAttr.split("\\s+");
		if (splitViewBox.length != 4) {
			return DEFAULT_DIMENSIONS;
		}
		try {
			int viewBoxWidth = Integer.parseInt(splitViewBox[2]);
			int viewBoxHeight = Integer.parseInt(splitViewBox[3]);

			if (width == null && height == null) {
				width = viewBoxWidth;
				height = viewBoxHeight;
			} else if (width == null) {
				width = (int) Math.round(((double) height)
						* ((double) viewBoxWidth) / ((double) viewBoxHeight));
			} else if (height == null) {
				height = (int) Math.round(((double) width)
						* ((double) viewBoxHeight) / ((double) viewBoxWidth));
			}
			return new Point(width, height);
		} catch (NumberFormatException ex) {
			return DEFAULT_DIMENSIONS;
		}
	}

	private int parseOrDefault(String num, int def) {
		try {
			return Integer.parseInt(num);
		} catch (NumberFormatException e)
		{
			XRLog.general(Level.WARNING, "Invalid integer passed as dimension for SVG: " + num);
			return def;
		}
	}
	
	@Override
	public int getSVGWidth(Element e) {
		if (e.hasAttribute("width")) {
			return parseOrDefault(e.getAttribute("width"), Integer.parseInt(DEFAULT_VP_WIDTH));
		}
		else {
			return Integer.parseInt(DEFAULT_VP_WIDTH);
		}
	}

	@Override
	public int getSVGHeight(Element e) {
		if (e.hasAttribute("height")) {
			return parseOrDefault(e.getAttribute("height"), Integer.parseInt(DEFAULT_VP_HEIGHT));
		}
		else {
			return Integer.parseInt(DEFAULT_VP_HEIGHT);
		}
	}
}
