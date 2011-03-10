/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.access;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.projectforge.core.BaseDao;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.core.QueryFilter;
import org.projectforge.task.TaskDO;
import org.projectforge.task.TaskDao;
import org.projectforge.task.TaskNode;
import org.projectforge.task.TaskTree;
import org.projectforge.user.GroupDO;
import org.projectforge.user.GroupDao;
import org.projectforge.user.PFUserDO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AccessDao extends BaseDao<GroupTaskAccessDO>
{
  // private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AccessDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "task.title", "group.name"};

  private TaskTree taskTree;

  private TaskDao taskDao;

  private GroupDao groupDao;

  public void setTaskTree(TaskTree taskTree)
  {
    this.taskTree = taskTree;
  }

  public void setTaskDao(TaskDao taskDao)
  {
    this.taskDao = taskDao;
  }

  public AccessDao()
  {
    super(GroupTaskAccessDO.class);
  }

  public void setGroupDao(GroupDao groupDao)
  {
    this.groupDao = groupDao;
  }

  /**
   * @param access
   * @param taskId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final GroupTaskAccessDO access, Integer taskId)
  {
    TaskDO task = taskDao.getOrLoad(taskId);
    access.setTask(task);
  }

  /**
   * @param access
   * @param groupId If null, then group will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setGroup(final GroupTaskAccessDO access, Integer groupId)
  {
    GroupDO group = groupDao.getOrLoad(groupId);
    access.setGroup(group);
  }

  /**
   * Loads all GroupTaskAccessDO (not deleted ones) without any access checking.
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<GroupTaskAccessDO> internalLoadAll()
  {
    List<GroupTaskAccessDO> list = getHibernateTemplate().find(
        "from GroupTaskAccessDO g join fetch g.accessEntries where deleted=false order by g.task.id, g.group.id");
    list = selectUnique(list);
    return list;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public List<GroupTaskAccessDO> getList(final BaseSearchFilter filter)
  {
    final AccessFilter myFilter;
    if (filter instanceof AccessFilter) {
      myFilter = (AccessFilter) filter;
    } else {
      myFilter = new AccessFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getTaskId() != null) {
      List<Integer> descendants = null;
      List<Integer> ancestors = null;
      final TaskNode node = taskTree.getTaskNodeById(myFilter.getTaskId());
      if (myFilter.isRecursive() == true) {
        descendants = node.getDescendantIds();
      }
      if (myFilter.isInherit() == true) {
        ancestors = node.getAncestorIds();
      }
      if (descendants != null || ancestors != null) {
        final List<Integer> taskIds = new ArrayList<Integer>();
        if (descendants != null) {
          taskIds.addAll(descendants);
        }
        if (ancestors != null) {
          taskIds.addAll(ancestors);
        }
        taskIds.add(node.getId());
        queryFilter.add(Restrictions.in("task.id", taskIds));
      } else {
        queryFilter.add(Restrictions.eq("task.id", myFilter.getTaskId()));
      }
    }
    if (myFilter.getGroupId() != null) {
      final GroupDO group = new GroupDO();
      group.setId(myFilter.getGroupId());
      queryFilter.add(Restrictions.eq("group", group));
    }
    final List<GroupTaskAccessDO> list = getList(queryFilter);
    if (myFilter.getUserId() != null) {
      final List<GroupTaskAccessDO> result = new ArrayList<GroupTaskAccessDO>();
      for (final GroupTaskAccessDO access : list) {
        if (userGroupCache.isUserMemberOfGroup(myFilter.getUserId(), access.getGroupId())) {
          result.add(access);
        }
      }
      return result;
    }
    return list;
  }

  /**
   * @return Always true, no generic select access needed for group task access objects.
   * @see org.projectforge.core.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @return false, if no admin user and the context user is not member of the group. Also deleted entries are only visible for admin users.
   * @see org.projectforge.core.BaseDao#hasSelectAccess(org.projectforge.core.BaseDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final GroupTaskAccessDO obj, final boolean throwException)
  {
    Validate.notNull(obj);
    boolean result = accessChecker.isUserMemberOfAdminGroup(user);
    if (result == false && obj.isDeleted() == false) {
      Validate.notNull(user);
      result = userGroupCache.isUserMemberOfGroup(user.getId(), obj.getGroupId());
    }
    if (throwException == true && result == false) {
      throw new AccessException(AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  /**
   * @see org.projectforge.core.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final GroupTaskAccessDO obj, final GroupTaskAccessDO oldObj,
      final OperationType operationType, final boolean throwException)
  {
    return accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, operationType, throwException);
  }

  /**
   * @see org.projectforge.core.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final GroupTaskAccessDO obj, final GroupTaskAccessDO dbObj,
      final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getTaskId());
    Validate.notNull(obj.getTaskId());
    if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.UPDATE, throwException) == false) {
      return false;
    }
    if (dbObj.getTaskId().equals(obj.getTaskId()) == false) {
      // User moves the object to another task:
      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.INSERT, throwException) == false) {
        // Inserting of object under new task not allowed.
        return false;
      }
      if (accessChecker.hasPermission(user, dbObj.getTaskId(), AccessType.TASK_ACCESS_MANAGEMENT, OperationType.DELETE, throwException) == false) {
        // Deleting of object under old task not allowed.
        return false;
      }
    }
    return true;
  }

  /**
   * @see org.projectforge.core.BaseDao#prepareHibernateSearch(org.projectforge.core.ExtendedBaseDO, org.projectforge.access.OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final GroupTaskAccessDO obj, final OperationType operationType)
  {
    final TaskDO task = obj.getTask();
    if (task != null && Hibernate.isInitialized(task) == false) {
      Hibernate.initialize(obj.getTask());
      obj.setTask(taskTree.getTaskById(task.getId()));
    }
    final GroupDO group = obj.getGroup();
    if (group != null && Hibernate.isInitialized(group) == false) {
      obj.setGroup(userGroupCache.getGroup(obj.getGroupId()));
    }
  }

  @Override
  protected void afterSaveOrModify(GroupTaskAccessDO obj)
  {
    super.afterSaveOrModify(obj);
    taskTree.setGroupTaskAccess(obj);
  }

  @Override
  protected void afterUpdate(GroupTaskAccessDO obj, GroupTaskAccessDO dbObj)
  {
    List<AccessEntryDO> entries = obj.getOrderedEntries();
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (AccessEntryDO entry : entries) {
      if (first == true) {
        first = false;
      } else {
        buf.append(";");
      }
      buf.append(entry.getAccessType()).append("={").append(entry.getAccessSelect()).append(",").append(entry.getAccessInsert())
          .append(",").append(entry.getAccessUpdate()).append(",").append(entry.getAccessDelete()).append("}");
    }
    createHistoryEntry(obj, obj.getId(), "entries", String.class, "", buf.toString());
  }

  @Override
  protected void afterDelete(GroupTaskAccessDO obj)
  {
    taskTree.removeGroupTaskAccess(obj);
  }

  @Override
  protected void afterUndelete(GroupTaskAccessDO obj)
  {
    taskTree.setGroupTaskAccess(obj);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public GroupTaskAccessDO newInstance()
  {
    return new GroupTaskAccessDO();
  }
}
