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

package org.apache.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;

/**
 * A ResultIterator over the underlying JDBC ResultSet.
 * 
 * @since 1.2
 */
// Replaces DefaultResultIterator
public class JDBCResultIterator implements ResultIterator {

    // Connection information
    protected Connection connection;
    protected Statement statement;
    protected ResultSet resultSet;

    protected RowDescriptor rowDescriptor;

    DataRowPostProcessor postProcessor;

    // last indexed PK
    protected DbEntity rootEntity;
    protected int[] pkIndices;

    protected int mapCapacity;

    protected boolean closingConnection;
    protected boolean closed;

    protected boolean nextRow;

    private String[] labels;
    private int[] types;

    /**
     * Creates new JDBCResultIterator that reads from provided ResultSet.
     */
    public JDBCResultIterator(Connection connection, Statement statement,
            ResultSet resultSet, RowDescriptor descriptor)
            throws CayenneException {

        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.rowDescriptor = descriptor;

        this.mapCapacity = (int) Math.ceil((descriptor.getWidth()) / 0.75);

        checkNextRow();

        if (nextRow) {
            // extract column parameters to speed up processing...
            ColumnDescriptor[] columns = descriptor.getColumns();
            int width = columns.length;
            labels = new String[width];
            types = new int[width];

            for (int i = 0; i < width; i++) {
                labels[i] = columns[i].getLabel();
                types[i] = columns[i].getJdbcType();
            }
        }
    }

    /**
     * Returns all unread data rows from ResultSet, closing this iterator if needed.
     */
    public List<DataRow> dataRows(boolean close) throws CayenneException {
        List<DataRow> list = new ArrayList<DataRow>();

        try {
            while (hasNextRow()) {
                list.add((DataRow) nextDataRow());
            }
        }
        finally {
            if (close) {
                this.close();
            }
        }

        return list;
    }

    /**
     * Returns true if there is at least one more record that can be read from the
     * iterator.
     */
    public boolean hasNextRow() {
        return nextRow;
    }

    /**
     * Returns the next result row as a Map.
     */
    public Map<String, Object> nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // read
        Map<String, Object> row = readDataRow();

        // rewind
        checkNextRow();
        return row;
    }

    /**
     * Returns a map of ObjectId values from the next result row. Primary key columns are
     * determined from the provided DbEntity.
     * 
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    public Map<String, Object> nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // index id
        if (rootEntity != entity || pkIndices == null) {
            this.rootEntity = entity;
            indexPK();
        }

        // read ...
        // TODO: note a mismatch with 1.1 API - ID positions are preset and are
        // not affected by the entity specified (think of deprecating/replacing this)
        Map<String, Object> row = readIdRow();

        // rewind
        checkNextRow();

        return row;
    }

    /**
     * @since 3.0
     */
    public Object nextId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        // index id
        if (rootEntity != entity || pkIndices == null) {
            this.rootEntity = entity;
            indexPK();
        }

        Object id = readId();

        // rewind
        checkNextRow();
        return id;
    }

    public void skipDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }
        checkNextRow();
    }
    
    /**
     * Closes ResultIterator and associated ResultSet. This method must be called
     * explicitly when the user is finished processing the records. Otherwise unused
     * database resources will not be released properly.
     */
    public void close() throws CayenneException {
        if (!closed) {
            nextRow = false;

            StringBuffer errors = new StringBuffer();

            try {
                resultSet.close();
            }
            catch (SQLException e1) {
                errors.append("Error closing ResultSet.");
            }

            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException e2) {
                    errors.append("Error closing PreparedStatement.");
                }
            }

            // TODO: andrus, 5/8/2006 - closing connection within JDBCResultIterator is
            // obsolete as this is bound to transaction closing in DataContext. Deprecate
            // this after 1.2

            // close connection, if this object was explicitly configured to be
            // responsible for doing it
            if (connection != null && isClosingConnection()) {
                try {
                    connection.close();
                }
                catch (SQLException e3) {
                    errors.append("Error closing Connection.");
                }
            }

            if (errors.length() > 0) {
                throw new CayenneException("Error closing ResultIterator: "
                        + errors.toString());
            }

            closed = true;
        }
    }

    /**
     * Returns the number of columns in the result row.
     */
    public int getDataRowWidth() {
        return rowDescriptor.getWidth();
    }

    /**
     * Moves internal ResultSet cursor position down one row. Checks if the next row is
     * available.
     */
    protected void checkNextRow() throws CayenneException {
        nextRow = false;
        try {
            if (resultSet.next()) {
                nextRow = true;
            }
        }
        catch (SQLException e) {
            throw new CayenneException("Error rewinding ResultSet", e);
        }
    }

    /**
     * Reads a row from the internal ResultSet at the current cursor position.
     */
    protected Map<String, Object> readDataRow() throws CayenneException {
        try {
            DataRow dataRow = new DataRow(mapCapacity);
            ExtendedType[] converters = rowDescriptor.getConverters();

            int resultWidth = labels.length;

            // process result row columns,
            for (int i = 0; i < resultWidth; i++) {
                // note: jdbc column indexes start from 1, not 0 unlike everywhere else
                Object val = converters[i].materializeObject(resultSet, i + 1, types[i]);
                dataRow.put(labels[i], val);
            }

            if (postProcessor != null) {
                postProcessor.postprocessRow(resultSet, dataRow);
            }

            return dataRow;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException("Exception materializing column.", Util
                    .unwindException(otherex));
        }
    }

    protected Object readId() throws CayenneException {

        int len = pkIndices.length;

        if (len != 1) {
            return readIdRow();
        }

        ExtendedType[] converters = rowDescriptor.getConverters();

        try {

            // dereference column index
            int index = pkIndices[0];

            // note: jdbc column indexes start from 1, not 0 as in arrays
            Object val = converters[index].materializeObject(
                    resultSet,
                    index + 1,
                    types[index]);

            // note that postProcessor overrides are not applied. ID mapping must be the
            // same across inheritance hierarchy, so overrides do not make sense.
            return val;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException("Exception materializing id column.", Util
                    .unwindException(otherex));
        }
    }

    /**
     * Reads a row from the internal ResultSet at the current cursor position, processing
     * only columns that are part of the ObjectId of a target class.
     */
    protected Map<String, Object> readIdRow() throws CayenneException {
        try {
            DataRow idRow = new DataRow(2);
            ExtendedType[] converters = rowDescriptor.getConverters();
            int len = pkIndices.length;

            for (int i = 0; i < len; i++) {

                // dereference column index
                int index = pkIndices[i];

                // note: jdbc column indexes start from 1, not 0 as in arrays
                Object val = converters[index].materializeObject(
                        resultSet,
                        index + 1,
                        types[index]);
                idRow.put(labels[index], val);
            }

            if (postProcessor != null) {
                postProcessor.postprocessRow(resultSet, idRow);
            }

            return idRow;
        }
        catch (CayenneException cex) {
            // rethrow unmodified
            throw cex;
        }
        catch (Exception otherex) {
            throw new CayenneException("Exception materializing id column.", Util
                    .unwindException(otherex));
        }
    }

    /**
     * Creates an index of PK columns in the RowDescriptor.
     */
    protected void indexPK() {
        if (rootEntity == null) {
            throw new CayenneRuntimeException("Null root DbEntity, can't index PK");
        }

        int len = rootEntity.getPrimaryKeys().size();

        // sanity check
        if (len == 0) {
            throw new CayenneRuntimeException("Root DbEntity has no PK defined: "
                    + rootEntity);
        }

        int[] pk = new int[len];
        ColumnDescriptor[] columns = rowDescriptor.getColumns();
        for (int i = 0, j = 0; i < columns.length; i++) {
            DbAttribute a = (DbAttribute) rootEntity.getAttribute(columns[i].getName());
            if (a != null && a.isPrimaryKey()) {
                pk[j++] = i;
            }
        }

        this.pkIndices = pk;
    }

    /**
     * Returns <code>true</code> if this iterator is responsible for closing its
     * connection, otherwise a user of the iterator must close the connection after
     * closing the iterator.
     */
    public boolean isClosingConnection() {
        return closingConnection;
    }

    /**
     * Sets the <code>closingConnection</code> property.
     */
    public void setClosingConnection(boolean flag) {
        this.closingConnection = flag;
    }

    public RowDescriptor getRowDescriptor() {
        return rowDescriptor;
    }

    void setPostProcessor(DataRowPostProcessor postProcessor) {
        this.postProcessor = postProcessor;
    }
}
