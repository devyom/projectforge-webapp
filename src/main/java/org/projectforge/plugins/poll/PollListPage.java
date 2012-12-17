/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.poll;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DetachableDOModel;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;

/**
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@ListPage(editPage = PollEditPage.class)
public class PollListPage extends AbstractListPage<PollListForm, PollDao, PollDO> implements IListPageColumnsCreator<PollDO>
{
  private static final long serialVersionUID = 1749480610890950450L;

  @SpringBean(name = "pollDao")
  private PollDao PollDao;

  /**
   * 
   */
  public PollListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.poll");
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage, boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<PollDO>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<PollDO>> columns = new ArrayList<IColumn<PollDO>>();

    final CellItemListener<PollDO> cellItemListener = new CellItemListener<PollDO>() {
      @Override
      public void populateItem(final Item<ICellPopulator<PollDO>> item, final String componentId, final IModel<PollDO> rowModel)
      {
        final PollDO teamCal = rowModel.getObject();
        final StringBuffer cssStyle = getCssStyle(teamCal.getId(), teamCal.isDeleted());
        if (cssStyle.length() > 0) {
          item.add(AttributeModifier.append("style", new Model<String>(cssStyle.toString())));
        }
      }
    };

    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("plugins.poll.new.title"), getSortable("title", sortable), "title",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("plugins.poll.new.description"), getSortable("description", sortable),
        "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("plugins.poll.new.location"), getSortable("location", sortable),
        "location", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("plugins.teamcal.owner"), getSortable("owner", sortable),
        "owner.username", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("lastUpdate"), getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));
    return columns;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  protected PollDao getBaseDao()
  {
    return PollDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected PollListForm newListForm(final AbstractListPage< ? , ? , ? > parentPage)
  {
    return new PollListForm(this);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getModel(java.lang.Object)
   */
  @Override
  protected IModel<PollDO> getModel(final PollDO object)
  {
    final DetachableDOModel<PollDO, PollDao> model = new DetachableDOModel<PollDO, PollDao>(object, getBaseDao());
    return model;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#init()
   */
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
  }
}