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
package org.apache.cayenne.map;

/**
 * A component in a path chain.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public interface PathComponent<T extends Attribute, U extends Relationship> {

    T getAttribute();

    U getRelationship();

    String getName();

    /**
     * Returns a joint type of this path component in the expression. Attributes always
     * return undefined type, while relationships may be outer or inner joins.
     */
    JoinType getJoinType();

    boolean isLast();

    /**
     * Returns true if this component is an alias for a different path. Only the first
     * path component can be an alias. Aliased path can be obtained by calling
     * {@link #getAliasedPath()}.
     */
    boolean isAlias();

    /**
     * Returns an aliased path or null if this component is not an alias.
     */
    Iterable<PathComponent<T, U>> getAliasedPath();
}
