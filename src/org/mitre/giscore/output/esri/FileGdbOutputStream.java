/****************************************************************************************
 *  FileGdbOutputStream.java
 *
 *  Created: Dec 18, 2012
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2012
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
package org.mitre.giscore.output.esri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Row;
import org.mitre.giscore.events.Schema;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.filegdb.GDB;
import org.mitre.giscore.filegdb.Geodatabase;
import org.mitre.giscore.filegdb.Table;
import org.mitre.giscore.geometry.Circle;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.GeometryBag;
import org.mitre.giscore.geometry.Line;
import org.mitre.giscore.geometry.LinearRing;
import org.mitre.giscore.geometry.MultiLine;
import org.mitre.giscore.geometry.MultiLinearRings;
import org.mitre.giscore.geometry.MultiPoint;
import org.mitre.giscore.geometry.MultiPolygons;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.geometry.Polygon;
import org.mitre.giscore.output.FeatureKey;
import org.mitre.giscore.output.IContainerNameStrategy;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.utils.Pair;
import org.mitre.giscore.utils.ZipUtils;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Geodetic3DPoint;

/**
 * This class is using some mechanics form the parent class which was written
 * for ESRI's XML interchange. It is notable that it avoids setting the 
 * base classes' stream variable to allow the reuse of the parent classes' output
 * streams.
 * <p>
 * The parent class also contains a FeatureSorter which could take a good amount
 * of memory and which is not required for this direct writer. This class writes
 * using the FileGeodatabase API via a native method layer and therefore keeps
 * open pointers to all the used tables and writes directly to the tables. The
 * feature sorter is still needed as it tracks the schemas.
 *
 * @author DRAND
 *
 */
public class FileGdbOutputStream extends XmlGdbOutputStream implements
		IGISOutputStream, FileGdbConstants {	  
	private boolean deleteOnClose;
	private IContainerNameStrategy containerNameStrategy;
	private OutputStream outputStream;
	private File outputPath;
	private Geodatabase database;
	private Map<String, Table> tables = new HashMap<String, Table>();
	private AtomicInteger nid = new AtomicInteger();
	/**
	 * Maps the schema name to the schema. The schemata included are both
	 * defined schemata as well as implied or inline schemata that are defined
	 * with their data.
	 */
	private Map<URI, Schema> schemata = new HashMap<URI, Schema>();
	/**
	 * Maps a set of simple fields, derived from inline data declarations to a
	 * schema. This is used to gather like features together. THe assumption is
	 * that we will see consistent elements between features.
	 */
	private Map<Set<SimpleField>, Schema> internalSchema = null;
	private ArrayList<Object> outputList;

	/**
	 * Ctor
	 * 
	 * @param stream
	 *            the output stream to write the resulting GDB into, never
	 *            <code>null</code>.
	 * @param path
	 *            the directory and file that should hold the file gdb, never
	 *            <code>null</code>.
	 * @param containerNameStrategy
	 *            a name strategy to override the default, may be
	 *            <code>null</code>.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public FileGdbOutputStream(OutputStream stream, File path,
			IContainerNameStrategy containerNameStrategy) throws IOException {
		if (stream == null) {
			throw new IllegalArgumentException("stream should never be null");
		}
		if (path == null || !path.getParentFile().exists()) {
			path = null;
			deleteOnClose = true;
			File temp = new File(System.getProperty("java.io.tmpdir"));
			long t = System.currentTimeMillis();
			path = new File(temp, "result" + t + ".gdb");
		}
		if (containerNameStrategy == null) {
			this.containerNameStrategy = new BasicContainerNameStrategy();
		} else {
			this.containerNameStrategy = containerNameStrategy;
		}
		outputStream = stream;
		outputPath = path;
		database = new Geodatabase(outputPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		// close tables
		for (Table table : tables.values()) {
			table.close();
		}
		// close geodatabase
		database.close();

		// zip and stream
		ZipUtils.outputZipComponents(outputPath.getName(), outputPath,
				(ZipOutputStream) outputStream);

		if (deleteOnClose) {
			deleteDirContents(outputPath);
			outputPath.delete();
		}
		
		// Close original outputStream
		if (outputStream != null) {
			IOUtils.closeQuietly(outputStream);
			outputStream = null;
		}
	}

	/**
	 * delete dir content
	 * 
	 * @param directory
	 */
	private void deleteDirContents(File directory) {
		if (directory != null) {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					deleteDirContents(file);
				}
				file.delete();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.IGISOutputStream#write(org.mitre.giscore.events
	 * .IGISObject)
	 */
	@Override
	public void write(IGISObject object) {
		object.accept(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.events
	 * .Row)
	 */
	@Override
	public void visit(Row row) {
		try {
			String fullpath = getFullPath();
			FeatureKey featureKey = new FeatureKey(sorter.getSchema(row), fullpath,
					null, row.getClass());
			checkAndRegisterKey(fullpath, featureKey);
			Table table = tables.get(fullpath);
			if (table == null) {
				String descriptor = createDescriptor(featureKey, false, row);
				table = database.createTable(getParentPath(), descriptor);
				tables.put(fullpath, table);
			}
			org.mitre.giscore.filegdb.Row tablerow = table.createRow();
			transferData(tablerow, row);
			table.add(tablerow);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void transferData(org.mitre.giscore.filegdb.Row tablerow, Row row) {
		Map<String,Object> datamap = new HashMap<String, Object>();
		for(SimpleField field : row.getFields()) {
			Object data = row.getData(field);
			if (data == null) {
				datamap.put(field.getName(), GDB.NULL_OBJECT);
			} else {
				datamap.put(field.getName(), data);
			}
		}
		tablerow.setAttributes(datamap);
	}

	/**
	 * This row has inline data in the extended data, so extract the field names
	 * and create a set of such fields.
	 * 
	 * @param feature
	 *            the feature
	 * @return the fields, may be empty
	 */
	private Set<SimpleField> getFields(Row row) {
		Set<SimpleField> rval = new HashSet<SimpleField>();
		for (SimpleField field : row.getFields()) {
			rval.add(field);
		}
		return rval;
	}

	// On I/O of Geo data from the Java layer:
	// 
	// For all representations there is an intial Short which holds the
	// type from Shape Types. The point entries are either two or three
	// doubles depending on whether the z-axis has a value.
	//
	//		  Short: # (0 = Point, 1 = MultiPoint, 2 = Line/Polyline, 3 = Ring/Polygon)
	//		  Boolean: hasz (true if 3D points)
	//		  Integer: npoints
	//		  Integer: nparts
	//		  part array (may be empty if nparts == 0)
	//		  Double long, lat, zelev 
	
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
			String fullpath = getFullPath();
			String parentpath = getParentPath();
			FeatureKey featureKey = new FeatureKey(sorter.getSchema(feature), fullpath,
					feature.getGeometry().getClass(), feature.getClass());
			checkAndRegisterKey(fullpath, featureKey);
			Table table = tables.get(fullpath);
			if (table == null) {
				String descriptor = createDescriptor(featureKey, true, feature);
				table = Table.createTable(database, parentpath, descriptor);
				tables.put(fullpath, table);
			}
			org.mitre.giscore.filegdb.Row tablerow = table.createRow();
			transferData(tablerow, feature);
			// Encode the geometry
			outputList = new ArrayList<Object>(10);
			Geometry geo = feature.getGeometry();
			geo.accept(this);
			tablerow.setGeometry(outputList.toArray());
			outputList = null;
			table.add(tablerow);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getParentPath() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++) {
			sb.append("\\");
			sb.append(path.get(i));
		}
		if (sb.toString().length() == 0) {
			sb.append("\\");
		}
		return sb.toString().replaceAll("\\s+", "_");
	}

	/**
	 * If the feature or row is presented without a container, we need to 
	 * derive a pseudo container name from the schema instead.
	 * 
	 * @param row
	 */
	private String getNameFromRow(Row row) {
		String rval;
		if (row.getSchema() != null) {
			rval = row.getSchema().toASCIIString();
			rval = rval.replaceAll("[\\p{Punct}+\\s+]", "_");
		} else {
			rval = "feature_" + nid.incrementAndGet();
		}
		if (row instanceof Feature)  {
			Feature f = (Feature) row;
			rval += "_";
			rval += f.getGeometry().getClass().getSimpleName();
		}
		return rval;
	}

	private String createDescriptor(FeatureKey featureKey, boolean isFeature, Row row)
			throws XMLStreamException, IOException {
		stream = new ByteArrayOutputStream(2000);
		init(stream, "UTF8");
		String datasetname = datasets.get(featureKey);
		if (datasetname == null || datasetname.equals("\\")) {
			datasetname = "\\" + getNameFromRow(row);
		}
		writeDataSetDef(featureKey, datasetname, 
				row instanceof Row ? ElementType.TABLE : ElementType.FEATURE_CLASS);
		writer.writeEndDocument();
		closeWriter();
		return new String(((ByteArrayOutputStream) stream).toByteArray(), "UTF8");
	}

	private void closeWriter() throws XMLStreamException, IOException {
		writer.flush();
		writer.close();
		stream.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Point)
	 */
	@Override
	public void visit(Point point) {
		if (hasNoPoints(point)) return;
		outputPartsAndPoints(shapePoint, shapePointZ, point);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiPoint)
	 */
	@Override
	public void visit(MultiPoint multiPoint) {
		if (hasNoPoints(multiPoint)) return;
		
		outputPartsAndPoints(shapeMultipoint, shapeMultipointZ, multiPoint);
	}
	
	public boolean hasNoPoints(Geometry geo) {
		List<Point> points = geo.getPoints();
		if (points.isEmpty()) { 
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Line)
	 */
	@Override
	public void visit(Line line) {
		if (hasNoPoints(line)) return;
		outputPartsAndPoints(shapePolyline, shapePolylineZ, line);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .GeometryBag)
	 */
	@Override
	public void visit(GeometryBag geobag) {
		throw new UnsupportedOperationException("Geometry Bag is not supported by FileGDB (at least at this time)"); 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiLine)
	 */
	@Override
	public void visit(MultiLine multiLine) {
		if (hasNoPoints(multiLine)) return;
		outputPartsAndPoints(shapePolyline, shapePolylineZ, multiLine);
	}
	
	/**
	 * Output common information for all complex geometries
	 * @param geo
	 */
	private void outputPartsAndPoints(Short type, Short type3d, Geometry geo) {
		int nparts = geo.getNumParts();
		int npoints = geo.getNumPoints();
		boolean hasz = geo.getCenter() instanceof Geodetic3DPoint;
		
		outputList.add(hasz ? type3d : type);
		outputList.add(hasz);
		outputList.add(npoints);
		outputList.add(nparts);
		int index = 0;
		for(int i = 0; i < geo.getNumParts(); i++) {
			Geometry sub = geo.getPart(i);
			outputList.add(index);
			index += sub.getNumPoints();
		}
		for(Point p : geo.getPoints()) {
			Geodetic2DPoint center = p.getCenter();
			outputList.add(center.getLongitude().inDegrees());
			outputList.add(center.getLatitude().inDegrees());
			if (hasz) {
				outputList.add(((Geodetic3DPoint) center).getElevation());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .LinearRing)
	 */
	@Override
	public void visit(LinearRing ring) {
		if (hasNoPoints(ring)) return;
		outputPartsAndPoints(shapePolygon, shapePolygonZ, ring);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiLinearRings)
	 */
	@Override
	public void visit(MultiLinearRings rings) {
		for(LinearRing r : rings.getLinearRings()) {
			r.accept(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .Polygon)
	 */
	@Override
	public void visit(Polygon polygon) {
		if (hasNoPoints(polygon)) return;
		outputPartsAndPoints(shapePolygon, shapePolygonZ, polygon);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mitre.giscore.output.StreamVisitorBase#visit(org.mitre.giscore.geometry
	 * .MultiPolygons)
	 */
	@Override
	public void visit(MultiPolygons polygons) {
		for(Polygon p : polygons.getPolygons()) {
			p.accept(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.output.esri.XmlGdbOutputStream#visit(org.mitre.giscore.events.ContainerStart)
	 */
	@Override
	public void visit(ContainerStart containerStart) {
		super.visit(containerStart);
		
		try {
			String datasetdoc = database.getDatasetDefinition(getParentPath(), DATASET_TYPE);
			
			if (StringUtils.isEmpty(datasetdoc)) {
				stream = new ByteArrayOutputStream(2000);
				init(stream, "UTF8");
				writer.writeStartDocument();
				writeDataSetDef(null, getFullPath(), ElementType.FEATURE_DATASET);
				writer.writeEndDocument();
				closeWriter();
				datasetdoc = new String(((ByteArrayOutputStream) stream).toByteArray(), "UTF8");
				
				database.createFeatureDataset(datasetdoc);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
}