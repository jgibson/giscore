/****************************************************************************************
 *  KmlOutputStream.java
 *
 *  Created: Jan 30, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.output.kml;

import java.awt.Color;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.events.BaseStart;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.DocumentStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.GroundOverlay;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.NetworkLink;
import org.mitre.giscore.events.Overlay;
import org.mitre.giscore.events.PhotoOverlay;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.ScreenLocation;
import org.mitre.giscore.events.ScreenOverlay;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.events.Style;
import org.mitre.giscore.events.StyleMap;
import org.mitre.giscore.events.TaggedMap;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;

/**
 * The kml output stream creates a result KML file using the given output
 * stream. It uses STaX methods for writing the XML elements to avoid building
 * an in-memory DOM, which reduces the memory overhead of creating the document.
 * <p>
 * For KML, each incoming element generally adds another full element to the
 * output document. There are a couple of distinct exceptions. These are the
 * Style selectors. The style selectors instead appear before the matched
 * feature, and the KML output stream buffers these until the next feature is
 * seen. At that point the styles are output after the element's attributes and
 * before any content.
 * 
 * @author DRAND
 * 
 */
public class KmlOutputStream extends XmlOutputStreamBase implements	IKml {
	private List<IGISObject> waitingElements = new ArrayList<IGISObject>();

	/**
	 * Ctor
	 * 
	 * @param stream
	 * @throws XMLStreamException
	 */
	public KmlOutputStream(OutputStream stream) throws XMLStreamException {
		super(stream);
		
		writer.writeStartDocument();
		writer.writeStartElement(KML);
		writer.writeDefaultNamespace(KML_NS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerEnd)
	 */
	@Override
	public void visit(ContainerEnd containerEnd) {
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		try {
			String tag = containerStart.getType();
			writer.writeStartElement(tag);
			handleAttributes(containerStart);
			handleWaitingElements();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Common code for outputting feature data that is held for both containers
	 * and other features like Placemarks and Overlays.
	 * 
	 * @param feature
	 */
	private void handleAttributes(BaseStart feature) {
		try {
			if (feature.getName() != null) {
				handleSimpleElement(NAME, feature.getName());
			}
			if (feature.getDescription() != null) {
				handleSimpleElement(DESCRIPTION, feature.getDescription());
			}
			Iterator<String> kiter = feature.keys();
			while (kiter.hasNext()) {
				String key = kiter.next();
				Object val = feature.get(key);
				handleSimpleElement(key, val.toString());
			}
			
			if (feature.hasExtendedData()) {
				String schema = feature.getSchema();
				writer.writeStartElement(EXTENDED_DATA);
				if (schema == null) {
					for (String fieldname : feature.getFieldNames()) {
						Object value = feature.getData(fieldname);
						writer.writeStartElement(DATA);
						writer.writeAttribute("name", fieldname);
						handleSimpleElement(VALUE, value.toString());
						writer.writeEndElement();
					}
				} else {
					for (String fieldname : feature.getFieldNames()) {
						Object value = feature.getData(fieldname);
						writer.writeStartElement(SCHEMA_DATA);
						writer.writeAttribute(SCHEMA_URL, schema);
						writer.writeStartElement(SIMPLE_DATA);
						writer.writeAttribute(NAME, fieldname);
						handleCharacters(value.toString());
						writer.writeEndElement();
						writer.writeEndElement();
					}
				}
				writer.writeEndElement();
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .DocumentStart)
	 */
	@Override
	public void visit(DocumentStart documentStart) {
		// Ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Feature)
	 */
	@Override
	public void visit(Feature feature) {
		try {
			String tag = feature.getType();
			writer.writeStartElement(tag);
			handleAttributes(feature);
			handleWaitingElements();
			if (feature instanceof Overlay) {
				handleOverlay((Overlay) feature);
			} else if (feature.getGeometry() != null) {
				handleGeometry(feature);
			} else if (feature instanceof NetworkLink) {
				handleNetworkLink((NetworkLink) feature);
			}
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Handle elements specific to a network link feature.
	 * @param link
	 */
	private void handleNetworkLink(NetworkLink link) throws XMLStreamException {
		handleTagElement(link.getLink());
	}

	/**
	 * Handle elements specific to an overlay feature
	 * @param overlay
	 * @throws XMLStreamException 
	 */
	private void handleOverlay(Overlay overlay) throws XMLStreamException {
		handleColor(COLOR, overlay.getColor());
		handleSimpleElement(DRAW_ORDER, Integer.toString(overlay.getDrawOrder()));
		handleTagElement(overlay.getIcon());
		
		if (overlay instanceof GroundOverlay) {
			GroundOverlay go = (GroundOverlay) overlay;
			handleSimpleElement(ALTITUDE, go.getAltitude());
			handleSimpleElement(ALTITUDE_MODE, go.getAltitudeMode());
			writer.writeStartElement(LAT_LON_BOX);
			handleSimpleElement(NORTH, go.getNorth());
			handleSimpleElement(SOUTH, go.getSouth());
			handleSimpleElement(EAST, go.getEast());
			handleSimpleElement(WEST, go.getWest());
			handleSimpleElement(ROTATION, go.getRotation());
			writer.writeEndElement();
		} else if (overlay instanceof PhotoOverlay) {
			// PhotoOverlay po = (PhotoOverlay) overlay;
			// TODO: Fill in sometime
		} else if (overlay instanceof ScreenOverlay) {
			ScreenOverlay so = (ScreenOverlay) overlay;
			handleXY(OVERLAY_XY, so.getOverlay());
			handleXY(SCREEN_XY, so.getScreen());
			handleXY(ROTATION_XY, so.getRotation());
			handleXY(SIZE, so.getSize());
			handleSimpleElement(ROTATION, so.getRotationAngle());
		}
	}

	/**
	 * Handle the screen location information
	 * 
	 * @param tag
	 * @param loc
	 * @throws XMLStreamException 
	 */
	private void handleXY(String tag, ScreenLocation loc) throws XMLStreamException {
		if (loc != null) {
			writer.writeStartElement(tag);
			writer.writeAttribute("x", Double.toString(loc.x));
			writer.writeAttribute("y", Double.toString(loc.y));
			writer.writeAttribute("xunits", loc.xunit.kmlValue);
			writer.writeAttribute("yunits", loc.yunit.kmlValue);
			writer.writeEndElement();
		}
	}

	/**
	 * Output a tagged element.
	 * @param data
	 */
	private void handleTagElement(TaggedMap data) throws XMLStreamException {
		if (data == null) return;
		writer.writeStartElement(data.getTag());
		for(String key : data.keySet()) {
			String value = data.get(key);
			handleSimpleElement(key, value);
		}
		writer.writeEndElement();
	}

	/**
	 * Handle elements that have been deferred. Style information is stored
	 * as found and output on the next feature or container.
	 * @throws XMLStreamException 
	 */
	private void handleWaitingElements() throws XMLStreamException {
		for(int i = waitingElements.size() - 1; i >= 0; i--) {
			IGISObject element = waitingElements.get(i);
			if (element instanceof Style) {
				handle((Style) element);
			} else if (element instanceof StyleMap) {
				handle((StyleMap) element);
			} else {
				throw new RuntimeException("Unknown kind of deferred element: " + element.getClass());
			}
		}
		waitingElements.clear();
	}
	
	/**
	 * @param feature
	 */
	private void handleGeometry(Feature feature) {
		try {
			Geometry geo = feature.getGeometry();
			if (geo == null) return;
			if (geo instanceof Point) {
				Point p = (Point) geo;
				writer.writeStartElement(POINT);
				handleSimpleElement(COORDINATES,
						handleCoordinates(Collections.singletonList(p)));
				writer.writeEndElement();
			} else if (geo instanceof Line) {
				Line l = (Line) geo;
				writer.writeStartElement(LINE_STRING);
				handleSimpleElement(COORDINATES,
						handleCoordinates(l.getPoints()));
				writer.writeEndElement();
			} else if (geo instanceof LinearRing) {
				LinearRing r = (LinearRing) geo;
				writer.writeStartElement(LINEAR_RING);
				handleSimpleElement(COORDINATES,
						handleCoordinates(r.iterator()));
				writer.writeEndElement();
			} else if (geo instanceof Polygon) {
				Polygon poly = (Polygon) geo;
				writer.writeStartElement(POLYGON);
				if (poly.getOuterRing() != null) {
					writer.writeStartElement(OUTER_BOUNDARY_IS);
					writer.writeStartElement(LINEAR_RING);
					handleSimpleElement(COORDINATES,
							handleCoordinates(poly.getOuterRing().iterator()));
					writer.writeEndElement();
					writer.writeEndElement();
				}
				if (poly.getLinearRings() != null) {
					for(LinearRing lr : poly.getLinearRings()) {
						writer.writeStartElement(INNER_BOUNDARY_IS);
						writer.writeStartElement(LINEAR_RING);
						handleSimpleElement(COORDINATES,
								handleCoordinates(lr.getPoints()));
						writer.writeEndElement();
						writer.writeEndElement();
					}
				}
				writer.writeEndElement();
			} else {
				throw new UnsupportedOperationException("Found unknown type of geometry: " + geo.getClass());
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * output the coordinates. The coordinates are output as lon,lat[,altitude]
	 * and are separated by spaces
	 * @param coordinates an iterator over the points, never <code>null</code>
	 * @throws XMLStreamException
	 * @return the coordinates as a string 
	 */
	private String handleCoordinates(Iterator<Point> coordinates) 
	throws XMLStreamException {
		StringBuilder b = new StringBuilder();
		while(coordinates.hasNext()) {
			Point point = coordinates.next();
			handleSingleCoordinate(b, point);
		}
		return b.toString();
	}
	
	/**
	 * output the coordinates. The coordinates are output as lon,lat[,altitude]
	 * and are separated by spaces
	 * @param coordinateList the list of coordinates, never <code>null</code>
	 * @throws XMLStreamException 
	 */
	private String handleCoordinates(Collection<Point> coordinateList) 
	throws XMLStreamException {
		StringBuilder b = new StringBuilder();
		for(Point point : coordinateList) {
			handleSingleCoordinate(b, point);
		}
		return b.toString();
	}

	/**
	 * Output a single coordinate
	 * @param b
	 * @param point
	 */
	private void handleSingleCoordinate(StringBuilder b, Point point) {
		if (b.length() > 0) {
			b.append(' ');
		}
		Geodetic2DPoint p2d = (Geodetic2DPoint) point.getCenter();
		b.append(p2d.getLongitude().inDegrees());
		b.append(',');
		b.append(p2d.getLatitude().inDegrees());
		if (point.getCenter() instanceof Geodetic3DPoint) {
			Geodetic3DPoint p3d = (Geodetic3DPoint) point.getCenter();
			b.append(',');
			b.append(p3d.getElevation());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Schema)
	 */
	@Override
	public void visit(Schema schema) {
		try {
			writer.writeStartElement(SCHEMA);
			writer.writeAttribute(NAME, schema.getName());
			writer.writeAttribute(ID, schema.getId());
			for(String name : schema.getKeys()) {
				SimpleField field = schema.get(name);
				if (field.getType().isGeometry()) {
					continue; // Skip geometry elements, no equivalent in Kml
				}
				writer.writeStartElement(SIMPLE_FIELD);
				if (field.getType().isKmlCompatible())
					writer.writeAttribute(TYPE, field.getType().toString().toLowerCase());
				else
					writer.writeAttribute(TYPE, "string");
				writer.writeAttribute(NAME, field.getName());
				if (StringUtils.isNotEmpty(field.getDisplayName())) {
					handleSimpleElement(DISPLAY_NAME, field.getDisplayName());
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Style)
	 */
	@Override
	public void visit(Style style) {
		waitingElements.add(style);
	}
	
	/**
	 * Actually output the style
	 * @param style
	 * @throws XMLStreamException 
	 */
	private void handle(Style style) throws XMLStreamException {
		writer.writeStartElement(STYLE);
		if (style.getId() != null) {
			writer.writeAttribute(ID, style.getId());
		}
		if (style.hasIconStyle()) {
			handleIconStyleElement(style);
		}
		if (style.hasLineStyle()) {
			handleLineStyleElement(style);
		}
		if (style.hasBalloonStyle()) {
			handleBalloonStyleElement(style);
		}
		if (style.hasLabelStyle()) {
			handleLabelStyleElement(style);
		}
		if (style.hasPolyStyle()) {
			handlePolyStyleElement(style);
		}
		writer.writeEndElement();
	}
	
	/**
	 * @param style
	 * @throws XMLStreamException 
	 */
	private void handlePolyStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(POLY_STYLE);
		handleColor(COLOR, style.getPolyColor());
		handleSimpleElement(FILL, style.isPolyfill() ? "1" : "0");
		handleSimpleElement(OUTLINE, style.isPolyoutline() ? "1" : "0");
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @throws XMLStreamException 
	 */
	private void handleLabelStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(LABEL_STYLE);
		handleColor(COLOR, style.getLabelColor());
		handleSimpleElement(SCALE, style.getLabelScale());
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @throws XMLStreamException 
	 */
	private void handleBalloonStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(BALLOON_STYLE);
		handleColor(BG_COLOR, style.getBalloonBgColor());
		handleSimpleElement(DISPLAY_MODE, style.getBalloonDisplayMode());
		handleSimpleElement(TEXT, style.getBalloonText());
		handleColor(TEXT_COLOR, style.getBalloonTextColor());
		writer.writeEndElement();
	}

	/**
	 * @param style
	 * @return
	 * @throws XMLStreamException 
	 */
	protected void handleLineStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(LINE_STYLE);
		handleSimpleElement(WIDTH, Double.toString(style.getLineWidth()));
		handleColor(COLOR, style.getLineColor());
		writer.writeEndElement();
	}
	
	/**
	 * @return
	 * @throws XMLStreamException 
	 */
	protected void handleIconStyleElement(Style style) throws XMLStreamException {
		writer.writeStartElement(ICON_STYLE);
		handleColor(COLOR, style.getIconColor());
		handleSimpleElement(SCALE, Double.toString(style.getIconScale()));
		if (style.getIconUrl() != null) {
			writer.writeStartElement(ICON);
			handleSimpleElement(HREF, style.getIconUrl());
			writer.writeEndElement();
		}
		writer.writeStartElement(HOT_SPOT);
		writer.writeAttribute("x", "0");
		writer.writeAttribute("y", "0");
		writer.writeAttribute("xunits", "0");
		writer.writeAttribute("yunits", "0");
		writer.writeEndElement();
		writer.writeEndElement();
	}
	
	/**
	 * Get the kml compliant color translation
	 * 
	 * @return kml color element
	 * @throws XMLStreamException 
	 */
	protected void handleColor(String tag, Color color) throws XMLStreamException {
		if (color != null) {
			StringBuilder sb = new StringBuilder(8);
			Formatter formatter = new Formatter(sb, Locale.US);
			formatter.format("%02x%02x%02x%02x", color.getAlpha(), color.getBlue(),
					color.getGreen(), color.getRed());
			handleSimpleElement(tag, sb.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .StyleMap)
	 */
	@Override
	public void visit(StyleMap styleMap) {
		waitingElements.add(styleMap);
	}
	
	/**
	 * Actually handle style map
	 * @param styleMap
	 * @throws XMLStreamException 
	 */
	private void handle(StyleMap styleMap) throws XMLStreamException {
		writer.writeStartElement(STYLE_MAP);
		if (styleMap.getId() != null) {
			writer.writeAttribute(ID, styleMap.getId());
		}
		Iterator<String> kiter = styleMap.keys();
		while(kiter.hasNext()) {
			String key = kiter.next();
			String value = styleMap.get(key);
			writer.writeStartElement(PAIR);
			handleSimpleElement(KEY, key);
			handleSimpleElement(STYLE_URL, value);
			writer.writeEndElement();
		}
		writer.writeEndElement();		
	}
}
