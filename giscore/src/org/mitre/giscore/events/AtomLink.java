/****************************************************************************************
 *  Link.java
 *
 *  Created: Jul 19, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
import java.io.Serializable;
import java.net.URL;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.StringUtils;
import org.mitre.giscore.utils.IDataSerializable;
import org.mitre.giscore.utils.SimpleObjectInputStream;
import org.mitre.giscore.utils.SimpleObjectOutputStream;

/**
 * Represent an atom link including the relationship, type and href information.
 * 
 * @author DRAND
 */
public class AtomLink implements IDataSerializable, Serializable {
	private static final long serialVersionUID = 1L;
	private URL href;
	private MimeType type;
	private String rel;
	private String hreflang;
	
	/**
	 * Default ctor
	 */
	public AtomLink() {
	}
	
	/**
	 * Basic ctor
	 * @param href the link, never <code>null</code>
	 * @param rel the relationship, never <code>null</code> or empty
	 */
	public AtomLink(URL href, String rel) {
		if (href == null) {
			throw new IllegalArgumentException("href should never be null");
		}
		if (rel == null || rel.trim().length() == 0) {
			throw new IllegalArgumentException(
					"rel should never be null or empty");
		}
		this.href = href;
		this.rel = rel;
	}
	
	/**
	 * @return the href
	 */
	public URL getHref() {
		return href;
	}
	/**
	 * @param href the href to set
	 */
	public void setHref(URL href) {
		this.href = href;
	}
	/**
	 * @return the type
	 */
	public MimeType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(MimeType type) {
		this.type = type;
	}
	/**
	 * @return the rel
	 */
	public String getRel() {
		return rel;
	}
	/**
	 * @param rel the rel to set
	 */
	public void setRel(String rel) {
		this.rel = rel;
	}
	/**
	 * @return the hreflang
	 */
	public String getHreflang() {
		return hreflang;
	}
	/**
	 * @param hreflang the hreflang to set
	 */
	public void setHreflang(String hreflang) {
		this.hreflang = hreflang;
	}
	@Override
	public void readData(SimpleObjectInputStream in) throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		String hrefstr = in.readString();
		if (StringUtils.isNotBlank(hrefstr)) {
			href = new URL(hrefstr);
		}
		hreflang = in.readString();
		rel = in.readString();
		String mstr = in.readString();
		if (StringUtils.isNotBlank(mstr)) {
			try {
				type = new MimeType(mstr);
			} catch (MimeTypeParseException e) {
				throw new IOException("Could not read mimetype", e);
			}
		}
		
	}
	@Override
	public void writeData(SimpleObjectOutputStream out) throws IOException {
		out.writeString(href != null ? href.toExternalForm() : null);
		out.writeString(hreflang);
		out.writeString(rel);
		out.writeString(type.toString());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((href == null) ? 0 : href.hashCode());
		result = prime * result
				+ ((hreflang == null) ? 0 : hreflang.hashCode());
		result = prime * result + ((rel == null) ? 0 : rel.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AtomLink other = (AtomLink) obj;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		if (hreflang == null) {
			if (other.hreflang != null)
				return false;
		} else if (!hreflang.equals(other.hreflang))
			return false;
		if (rel == null) {
			if (other.rel != null)
				return false;
		} else if (!rel.equals(other.rel))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.match(other.type))
			return false;
		return true;
	}

	
}