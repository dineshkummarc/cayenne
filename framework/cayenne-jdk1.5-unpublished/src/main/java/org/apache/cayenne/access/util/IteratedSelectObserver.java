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


package org.apache.cayenne.access.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.query.Query;

/**
 * OperationObserver that is used to track the execution
 * of SelectQueries with results returned as ResultIterator.
 *  
 * @author Andrus Adamchik
 */
public class IteratedSelectObserver extends DefaultOperationObserver {
	protected ResultIterator resultIterator;

	public boolean isIteratedResult() {
		return true;
	}

	public void nextDataRows(Query query, List dataRows) {
		throw new CayenneRuntimeException("Results unexpectedly returned as list.");
	}

	public void nextDataRows(Query q, ResultIterator it) {
	    // don't call super - it closes the iterator
		resultIterator = it;
	}

	public ResultIterator getResultIterator() throws CayenneException {
		if (super.hasExceptions()) {
			StringWriter str = new StringWriter();
			PrintWriter out = new PrintWriter(str);
			super.printExceptions(out);

			try {
				out.close();
				str.close();
			} catch (IOException ioex) {
				// this should never happen
			}

			throw new CayenneException(
				"Error getting ResultIterator: " + str.getBuffer());
		}

		return resultIterator;
	}

}