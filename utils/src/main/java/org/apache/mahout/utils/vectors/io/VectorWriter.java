/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.utils.vectors.io;

import java.io.Closeable;
import java.io.IOException;

import org.apache.mahout.math.Vector;

public interface VectorWriter extends Closeable {
  /**
   * Write all values in the Iterable to the output
   * @param iterable The {@link Iterable} to loop over
   * @return the number of docs written
   * @throws IOException if there was a problem writing
   *
   */
  long write(Iterable<Vector> iterable) throws IOException;

  /**
   * Write out a vector
   *
   * @param vector The {@link org.apache.mahout.math.Vector} to write
   * @throws IOException
   */
  void write(Vector vector) throws IOException;
  
  /**
   * Write the first <code>maxDocs</code> to the output.
   * @param iterable The {@link Iterable} to loop over
   * @param maxDocs the maximum number of docs to write
   * @return The number of docs written
   * @throws IOException if there was a problem writing
   */
  long write(Iterable<Vector> iterable, long maxDocs) throws IOException;

}
