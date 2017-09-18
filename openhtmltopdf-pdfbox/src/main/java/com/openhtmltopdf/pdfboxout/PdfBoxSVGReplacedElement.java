package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

import java.awt.*;

public class PdfBoxSVGReplacedElement implements PdfBoxReplacedElement {
    private final Element e;
    private final SVGDrawer svg;
    private Point point = new Point(0, 0);
    private int _intrinsicWidth;
    private int _intrinsicHeight;
    private final int dotsPerPixel;
    
    public PdfBoxSVGReplacedElement(Element e, SVGDrawer svgImpl, int dotsPerPixel) {
        this.e = e;
        this.svg = svgImpl;
        this.dotsPerPixel = dotsPerPixel;
        
        Point dimensions = svgImpl.parseDimensions(e);
        this._intrinsicWidth = dimensions.x * dotsPerPixel;
        this._intrinsicHeight = dimensions.y * dotsPerPixel;
    }

    public void scale(int width, int height) {
        float setWidth, setHeight;

        if (width != -1) {
            setWidth = width;

            if (height == -1 && _intrinsicWidth != 0) {
                // Use the width ratio to set the height.
                setHeight = (int) (((float) setWidth / (float) _intrinsicWidth) * _intrinsicHeight);
            } else {
                setHeight = height;
            }
        } else if (height != -1) {
            setHeight = height;

            if (_intrinsicHeight != 0) {
                // Use the height ratio to set the width.
                setWidth = (int) (((float) setHeight / (float) _intrinsicHeight) * _intrinsicWidth);
            } else {
                setWidth = 0;
            }
        } else {
            setWidth = _intrinsicWidth;
            setHeight = _intrinsicHeight;
        }

        // TODO: Make this class immutable.
        this._intrinsicWidth = (int)setWidth;
        this._intrinsicHeight = (int)setHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return this._intrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this._intrinsicHeight;
    }

    @Override
    public Point getLocation() {
        return point;
    }

    @Override
    public void setLocation(int x, int y) {
        point.setLocation(x, y);
    }

    @Override
    public void detach(LayoutContext c) {
    }

    @Override
    public boolean isRequiresInteractivePaint() {
        return false;
    }

    @Override
    public boolean hasBaseline() {
        return false;
    }

    @Override
    public int getBaseline() {
        return 0;
    }

    @Override
    public void paint(RenderingContext c, PdfBoxOutputDevice outputDevice, BlockBox box) {
        svg.drawSVG(e, outputDevice, c, point.getX(), point.getY(), getIntrinsicWidth(), getIntrinsicHeight(), dotsPerPixel);
    }
}
