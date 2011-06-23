/****************************************************************************************
 *  ContainerStart.java
 *
 *  Created: Jan 26, 2009
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
package org.mitre.giscore.events;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.mitre.giscore.IStreamVisitor;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.apache.commons.lang.StringUtils;

/**
 * We've seen the start of a container. A container can hold zero or more features
 * and zero or more sub containers.
 * 
 * @author DRAND
 */
public class ContainerStart extends Common implements IContainerType {
	private static final long serialVersionUID = 1L;

	@NonNull
	private String type;

    private boolean open;

	private List<StyleSelector> styles;

    /**
	 * Empty ctor for data IO.  Constructor must be followed by call to {@code readData()}
     * to initialize the object instance otherwise object is invalid.
	 */
	public ContainerStart() {
		type = IKml.DOCUMENT; // default
	}
	
    /**
     * Constructs a container that can hold zero or more features or other containers.
     * <code>ContainerStart</code> should be followed by a matching <code>ContainerEnd</code> element. 
     * @param type Type of container
     * @throws IllegalArgumentException if type is null or empty 
     */
	public ContainerStart(String type) {
		setType(type);
	}

	/**
	 * @return the type, never null
	 */
    @NonNull
	public String getType() {
		return type;
	}

	/**
	 * Set type of container (e.g. Folder, Document)
	 * @param type the type to set
	 * @throws IllegalArgumentException if type is null or empty
	 */
	public void setType(String type) {
		if (StringUtils.isBlank(type)) {
			throw new IllegalArgumentException("type should never be null or empty");
		}
		this.type = type;
	}

	/**
	 * Specifies whether a Container (Document or Folder) appears closed or open when first loaded into the Places panel.
	 * false=collapsed (the default), true=expanded. This element applies only to Document, Folder, and NetworkLink. 
	 * @return whether the Container appears open or closed
	 */
    public boolean isOpen() {
        return open;
    }

	/**
	 * Set open flag.
	 * @param open True if container appears open when first loaded
	 */
    public void setOpen(boolean open) {
        this.open = open;
    }

	/**
	 * Gets style elements that are children for this container
	 * @return list of styles, read-only empty list is returned if no styles are defined
	 */
	@NonNull
	public List<StyleSelector> getStyles() {
		return styles == null ? Collections.<StyleSelector>emptyList() : styles;
	}

	/**
	 * Set the list of styles for this container.
	 * @param styles List of styles
	 */
	public void setStyles(List<StyleSelector> styles) {
		this.styles = styles;
	}

	/**
	 * Append style to list of styles. Initializes the list if this is the
	 * first style.
	 * @param style Style or StyleMap to add to container
	 */
	public void addStyle(StyleSelector style) {
		if (style != null) {
			if (styles == null) styles = new LinkedList<StyleSelector>();
			styles.add(style);
		}
	}

    /*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
        StringBuilder b = new StringBuilder(super.toString());
        if (b.length() != 0 && b.charAt(b.length() - 1) != '\n') {
            b.append('\n');
        }
        b.append("  type=").append(type);
        return b.toString(); 
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContainerStart other = (ContainerStart) obj;
		if (open != other.open)
			return false;
		if (!type.equals(other.type))
			return false;
		if (styles == null) {
			if (other.styles != null)
				return false;
		} else if (!styles.equals(other.styles))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + type.hashCode(); // never null
		result = prime * result + (open ? 1231 : 1237);
		result = prime * result + ((styles == null || styles.isEmpty())
				? 0 : styles.hashCode());
		return result;
	}

	public void accept(IStreamVisitor visitor) {
    	visitor.visit(this);
    }

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.BaseStart#readData(org.mitre.giscore.utils.SimpleObjectInputStream)
	 */
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		super.readData(in);
		type = in.readString();
		// readString should never return null but enforce our nonNull contract and check anyway
		if (type == null) type = IKml.DOCUMENT; // default
		styles = (List<StyleSelector>)in.readObjectCollection();
	}

	/* (non-Javadoc)
	 * @see org.mitre.giscore.events.BaseStart#writeData(org.mitre.giscore.utils.SimpleObjectOutputStream)
	 */
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {	
		super.writeData(out);
		out.writeString(type);
		out.writeObjectCollection(styles);
	}

}
