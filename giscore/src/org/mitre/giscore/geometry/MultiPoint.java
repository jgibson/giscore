/***************************************************************************
 * $Id$
 *
 * (C) Copyright MITRE Corporation 2006-2008
 *
 * The program is provided "as is" without any warranty express or implied,
 * including the warranty of non-infringement and the implied warranties of
 * merchantibility and fitness for a particular purpose.  The Copyright
 * owner will not be liable for any damages suffered by you as a result of
 * using the Program.  In no event will the Copyright owner be liable for
 * any special, indirect or consequential damages or lost profits even if
 * the Copyright owner has been advised of the possibility of their
 * occurrence.
 *
 ***************************************************************************/
package org.mitre.giscore.geometry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.itf.geodesy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MultiPoint class represents a list of geodetic Points for input and output in
 * GIS formats such as ESRI Shapefiles or Google Earth KML files.  In ESRI Shapefiles,
 * this object corresponds to a ShapeType of MultiPoint. This type of object does not
 * exist as a primitive in Google KML files, so it is just written as a list of Points.
 * <p/>
 * Note if have points of mixed dimensions then MultiPoint container is downgraded to 2d.
 *
 * @author Paul Silvey
 */
public class MultiPoint extends Geometry implements Iterable<Point> {
	
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MultiPoint.class);

    private List<Point> pointList;

    /**
     * This method returns an iterator for cycling through the geodetic Points in this MultiPoint.
     * This class supports use of Java 'for each' syntax to cycle through the geodetic Points.
     *
     * @return Iterator over geodetic Point objects.
     */
    public Iterator<Point> iterator() {
        return Collections.unmodifiableCollection(pointList).iterator();
    }

	/**
	 * This method returns the {@code Point}s in this {@code MultiPoint}.
	 * <br/>
	 * The returned collection is unmodifiable.
	 *
	 * @return Collection of the {@code Point} objects.
	 */
	public List<Point> getPoints() {
		return Collections.unmodifiableList(pointList);
	}
	
	/**
	 * Empty ctor for object io
	 */
	public MultiPoint() {
		//
	}

    /**
     * The Constructor takes a list of points and initializes a Geometry Object for this MultiPoint.
     *
     * @param pts List of Geodetic2DPoint point objects to use for the parts of this MultiPoint.
     * @throws IllegalArgumentException error if object is not valid.
     */
    public MultiPoint(List<Point> pts) throws IllegalArgumentException {
        if (pts == null || pts.size() < 1)
            throw new IllegalArgumentException("MultiPoint must contain at least 1 Point");
        init(pts);
    }

    /**
     * Initialize
     * @param pts
     */
	private void init(List<Point> pts) {
		// Make sure all the points have the same number of dimensions (2D or 3D)
        is3D = pts.get(0).is3D();
        for (Point p : pts) {
            if (is3D != p.is3D()) {
                log.info("MultiPoint points have mixed dimensionality: downgrading to 2d");
                is3D = false;
                break;
            }
        }
        pointList = pts;
	}

    /* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#computeBoundingBox()
	 */
	protected void computeBoundingBox() {
		Geodetic2DPoint gp = pointList.get(0).asGeodetic2DPoint();
		bbox = is3D ? new Geodetic3DBounds((Geodetic3DPoint) gp)
				: new Geodetic2DBounds(gp);
		for (Point p : pointList)
			bbox.include(p.asGeodetic2DPoint());
		// make bbox unmodifiable
		bbox = is3D ? new UnmodifiableGeodetic3DBounds((Geodetic3DBounds) bbox)
				: new UnmodifiableGeodetic2DBounds(bbox);
	}

	/**
     * Tests whether this MultiPoint geometry is a container for otherGeom's type.
     *
     * @param otherGeom the geometry from which to test if this is a container for
     * @return true if the geometry of this object is a "proper" container for otherGeom features
     *          which in this case is a Point.
     */
    public boolean containerOf(Geometry otherGeom) {
        return otherGeom instanceof Point;
    }

    /**
     * The toString method returns a String representation of this Object suitable for debugging
     *
     * @return String containing Geometry Object type, bounding coordintates, and number of parts.
     */
    public String toString() {
        return "MultiPoint within " + bbox + " consists of " + pointList.size() + " Points";
    }
    
    public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }
    
	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		int pcount = in.readInt();
		ArrayList<Point> plist = new ArrayList<Point>();
		for(int i = 0; i < pcount; i++) {
			plist.add((Point) in.readObject());
		}
		init(plist);
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.geometry.Geometry#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		super.writeData(out);
		out.writeInt(pointList != null ? pointList.size() : 0);
		if (pointList != null)
			for(Point p : pointList) {
				out.writeObject(p);
			}
	}

	@Override
	public int getNumParts() {
		return pointList != null ? pointList.size() : 0;
	}
	
	@Override
	public Geometry getPart(int i) {
		return pointList != null ? pointList.get(i) : null;
	}

	@Override
	public int getNumPoints() {
		return getNumParts(); // Happily the count of parts and points is the same
	}
}
