/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.map.DbEntity;

/**
 * Defines API of an iterator over the records returned as a result of SelectQuery
 * execution. Usually a ResultIterator is supported by an open java.sql.ResultSet,
 * therefore most of the methods would throw checked exceptions. ResultIterators must be
 * explicitly closed when the user is done working with them.
 * <p>
 * Result "rows", depending on the query, may be represented as scalar values, DataRows,
 * or Object[] arrays containing a mix of scalars and DataRows.
 * </p>
 */
public interface ResultIterator {

    /**
     * Returns all unread data rows from ResultSet and closes this iterator if asked to do
     * so.
     * 
     * @deprecated since 3.0 use {@link #allRows(boolean)}.
     */
    List<DataRow> dataRows(boolean close) throws CayenneException;

    /**
     * Returns all yet unread rows from ResultSet and closes this iterator if asked to do
     * so.
     * 
     * @since 3.0
     */
    List allRows(boolean close) throws CayenneException;

    /**
     * Returns true if there is at least one more record that can be read from the
     * iterator.
     */
    boolean hasNextRow() throws CayenneException;

    /**
     * Returns the next result row as a Map.
     * 
     * @deprecated since 3.0 use {@link #nextRow()}.
     */
    Map<String, Object> nextDataRow() throws CayenneException;

    /**
     * Returns the next result row that is, depending on the query, may be a scalar value,
     * a DataRow, or an Object[] array containing a mix of scalars and DataRows.
     * 
     * @since 3.0
     */
    Object nextRow() throws CayenneException;

    /**
     * Returns a map of ObjectId values from the next result row. Primary key columns are
     * determined from the provided DbEntity.
     * 
     * @since 1.1
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    Map<String, Object> nextObjectId(DbEntity entity) throws CayenneException;

    /**
     * Reads and returns an id column or columns for the current row DbEntity. If an
     * entity has a single column id, the return value is an Object matching the column
     * type (e.g. java.lang.Long). If an entity has a compound PK, the return value is a
     * DataRow.
     * 
     * @since 3.0
     */
    Object nextId() throws CayenneException;

    /**
     * Skips current data row instead of reading it.
     * 
     * @deprecated since 3.0 use {@link #skipRow()} instead.
     */
    void skipDataRow() throws CayenneException;

    /**
     * @since 3.0
     */
    void skipRow() throws CayenneException;

    /**
     * Closes ResultIterator and associated ResultSet. This method must be called
     * explicitly when the user is finished processing the records. Otherwise unused
     * database resources will not be released properly.
     */
    void close() throws CayenneException;

    /**
     * Returns the number of columns in the result row.
     * 
     * @since 1.0.6
     * @deprecated since 3.0 in favor of {@link #getResultSetWidth()}.
     */
    int getDataRowWidth();

    /**
     * Returns a number of columns in the underlying ResultSet.
     * 
     * @since 3.0
     */
    int getResultSetWidth();
}
