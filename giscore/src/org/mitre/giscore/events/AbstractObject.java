package org.mitre.giscore.events;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.utils.SimpleObjectInputStream;

import java.io.IOException;

/**
 * AbstractObject is a base IGISObject that simply has a unique id field.
 * Conceptually same as the KML AbstractObjectGroup element. 
 *  
 * @author Jason Mathews, MITRE Corp.
 * Date: Sep 15, 2009 10:50:00 AM
 */
public abstract class AbstractObject implements IDataSerializable, IGISObject {

	private static final long serialVersionUID = 1L;

    private String id;

    /**
     * @return the id to use to distinguish this object from another.
     * Value is either a non-empty string or <code>null</code>.
     */
    @CheckForNull
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set, if blank or empty string then null is assigned.
     * Whitespace is stripped from start and end of the string if present.
     */
    public void setId(String id) {
        if (id != null) {
            id = id.trim();
            if (id.length() == 0) id = null;
        }
        this.id = id;
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AbstractObject))
			return false;
		AbstractObject other = (AbstractObject) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
     * Returns a hash code for this <code>AbstractObject</code>.
     *
     * @return  a hash code value for this object, equal to the
     *          hash code of its <code>id</code> or
	 *          0 if <code>id</code> is <tt>null</tt>.
     */
	@Override
	public int hashCode() {
		return (id == null) ? 0 : id.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	public void readData(SimpleObjectInputStream in) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		id = in.readString();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.utils.IDataSerializable#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(id);
	}
    
}
