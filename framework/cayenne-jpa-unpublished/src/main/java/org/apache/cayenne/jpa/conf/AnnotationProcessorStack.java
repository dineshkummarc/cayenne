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


package org.apache.cayenne.jpa.conf;

import java.lang.reflect.AnnotatedElement;

/**
 * A context for the annotation processor with methods to inspect and modify the processor
 * stack and record conflicts.
 * 
 * @author Andrus Adamchik
 */
public interface AnnotationProcessorStack {

    void push(Object object);

    Object pop();

    Object peek();

    void recordConflict(AnnotatedElement element, Class annotatedType, String message);

    int depth();
}