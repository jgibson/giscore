/****************************************************************************************
 *  SimpleObjectInputStream.java
 *
 *  Created: Mar 23, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.utils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;

/**
 * Simplified stream that doesn't hold object references on input. This class
 * replaces <tt>java.io.ObjectInputStream</tt> with a stream-lined implementation.
 * An SimpleObjectInputStream deserializes primitive data and objects previously
 * written using an SimpleObjectOutputStream.
 *
 * <p>SimpleObjectInputStream is used to recover the state of those objects previously
 * serialized. Other uses include passing objects between hosts using a socket stream
 * or for marshaling and unmarshaling arguments and parameters in a remote communication
 * system or caching to disk.
 *
 * <p>Only objects that support the IDataSerializable interface can be read from streams.
 *
 * <p>The method <code>readObject</code> is used to read an object from the
 * stream.
 *
 * <p>Primitive data types can be read from the stream using the appropriate
 * method on DataInput.
 * 
 * @author DRAND
 * 
 */
public class SimpleObjectInputStream implements Closeable {
	static final int NULL = 0;
	static final int BOOL = 1;
	static final int SHORT = 2;
	static final int INT = 3;
	static final int LONG = 4;
	static final int FLOAT = 5;
	static final int DOUBLE = 6;
	static final int STRING = 7;
	static final int OBJECT_NULL = 8;
	static final int DATE = 9;
	
	// Sync with output
	private static final short UNCACHED = 1;
	private static final short INSTANCE = 2;
	private static final short REF = 3;
	
	private final DataInputStream stream;
	
	private final Map<Integer, Class<IDataSerializable>> classMap = new HashMap<Integer, Class<IDataSerializable>>();
	
	/**
	 * Objects that are references in the input stream. Used to reduce small
	 * counts of objects that are used thousands of times in the input stream.
	 */
	private final Map<String, Object> refs = new HashMap<String, Object>();
	
	/**
	 * Creates an SimpleObjectInputStream that reads from the specified InputStream.
	 * 
	 * @param	in input stream to read from, never null
	 * @throws IllegalArgumentException if in is null
	 */
	public SimpleObjectInputStream(InputStream in) {
		if (in == null) {
			throw new IllegalArgumentException("in should never be null");
		}
		stream = new DataInputStream(in);
	}

	/**
	 * Closes the input stream. Must be called to release any resources
     * associated with the stream.
	 */
	public void close() {
		IOUtils.closeQuietly(stream);
	}

	/**
	 * Read the next object from the stream
	 * 
	 * @return the next object, or <code>null</code> if the stream is empty.
	 *
	 * @exception IOException if an I/O error occurs
	 * @exception ClassNotFoundException if the class cannot be located
	 * @exception  IllegalAccessException  if the class or its nullary
     *               constructor is not accessible.
     * @exception  InstantiationException
     *               if this <code>Class</code> represents an abstract class,
     *               an interface, an array class, a primitive type, or void;
     *               or if the class has no nullary constructor;
     *               or if the instantiation fails for some other reason.
	 */
    @Nullable
	public Object readObject() throws ClassNotFoundException, IOException,
			InstantiationException, IllegalAccessException {
		try {
			short type = readShort();
			IDataSerializable rval = null;
			if (type == NULL) {
				return null;
			} else if (type == UNCACHED) {
				rval = readClass();
				if (rval == null) {
					throw new IllegalStateException("Couldn't reify class");
				}
				rval.readData(this);
			} else {
				String ref = readString();
				if (type == INSTANCE) {
					rval = readClass();
					if (rval == null) {
						throw new IllegalStateException("Couldn't reify class");
					}
					rval.readData(this);
					refs.put(ref, rval);
				} else {
					rval = (IDataSerializable) refs.get(ref);
				}
			}
			return rval;
		} catch(ClassNotFoundException e) {
			throw e;
		} catch(EOFException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private IDataSerializable readClass() throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		boolean classref = readBoolean();
		Class<IDataSerializable> clazz;
		if (classref) {
			int refid = readInt();
			if (refid == 0) {
				return null;
			} else {
				clazz = classMap.get(refid);
				// clazz may be null if reference is bogus
				if (clazz == null) return null;
			}
		} else {
			String className = readString();
			int refid = readInt();
			clazz = (Class<IDataSerializable>) Class.forName(className);
			classMap.put(refid, clazz);
		}
		return clazz.newInstance();
	}

	/**
	 * Read a collection of objects from the stream
	 * @return the collection of object, may be <code>null</code>
	 * @throws IOException if an I/O error occurs
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
    @Nullable
	public List<? extends IDataSerializable> readObjectCollection()
			throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		int count = readInt();
		if (count == 0) {
			return null;
		}
		List rval = new ArrayList(count);
		for (int i = 0; i < count; i++) {
			rval.add(readObject());
		}
		return rval;
	}

	/**
	 * @return the next scalar object from the stream
	 * @throws IOException if an I/O error occurs
	 */
    @Nullable
	public Object readScalar() throws IOException {
		int type = stream.readShort();
		switch (type) {
		case NULL:
			return null;
		case OBJECT_NULL:
			return ObjectUtils.NULL;
		case SHORT:
			return stream.readShort();
		case INT:
			return stream.readInt();
		case LONG:
			return stream.readLong();
		case DOUBLE:
			return stream.readDouble();
		case FLOAT:
			return stream.readFloat();
		case STRING:
			return readString();
		case BOOL:
			return stream.readBoolean();
		case DATE:
			return new Date(stream.readLong());
		default:
			throw new UnsupportedOperationException(
					"Found unsupported scalar enum " + type);
		}
	}

	/**
	 * Read a string from the data stream
	 * 
	 * @return The string read, possibly {@code null}.
	 * @throws IOException if an I/O error occurs
	 */
    @Nullable
	public String readString() throws IOException {
		boolean isnull = stream.readBoolean();
		if (isnull) {
			return null;
		} else {
			return stream.readUTF();
		}
	}

	/**
	 * @return the next long value
	 * @throws IOException if an I/O error occurs
	 */
	public long readLong() throws IOException {
		return stream.readLong();
	}

	/**
	 * @return the next int value
	 * @throws IOException if an I/O error occurs
	 */
	public int readInt() throws IOException {
		return stream.readInt();
	}

    /**
     * See the general contract of the <code>readByte</code>
     * method of <code>DataInput</code>.
	 * @return the next byte value
	 * @throws IOException if an I/O error occurs
	 */
	public int readByte() throws IOException {
		return stream.readByte();
	}

	/**
	 * @return the next boolean value
	 * @throws IOException if an I/O error occurs
	 */
	public boolean readBoolean() throws IOException {
		return stream.readBoolean();
	}

	/**
	 * @return the next double value
	 * @throws IOException if an I/O error occurs
	 */
	public double readDouble() throws IOException {
		return stream.readDouble();
	}

	/**
	 * @return the next short value
	 * @throws IOException if an I/O error occurs
	 */
	public short readShort() throws IOException {
		return stream.readShort();
	}
}
