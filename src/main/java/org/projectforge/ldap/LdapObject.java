/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.ldap;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class LdapObject<I extends Serializable>
{
  private String dn, commonName;

  private String organizationalUnit;

  public abstract I getId();

  /**
   * @return the cn
   */
  public String getCommonName()
  {
    return commonName;
  }

  /**
   * cn
   * @param commonName the cn to set
   * @return this for chaining.
   */
  public LdapObject<I> setCommonName(final String commonName)
  {
    this.commonName = commonName;
    return this;
  }

  /**
   * @return the dn
   */
  public String getDn()
  {
    return dn;
  }

  /**
   * @param dn the dn to set
   * @return this for chaining.
   */
  public void setDn(final String dn)
  {
    this.dn = dn;
  }

  /**
   * @return the organizationalUnit
   */
  public String getOrganizationalUnit()
  {
    return organizationalUnit;
  }

  /**
   * @param organizationalUnit the organizationalUnit to set
   * @return this for chaining.
   */
  public void setOrganizationalUnit(final String organizationalUnit)
  {
    this.organizationalUnit = organizationalUnit;
  }

  @Override
  public boolean equals(final Object obj)
  {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public int hashCode()
  {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString()
  {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
