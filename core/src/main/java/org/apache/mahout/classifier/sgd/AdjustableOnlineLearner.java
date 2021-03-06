/*
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

package org.apache.mahout.classifier.sgd;

import org.apache.hadoop.io.Writable;
import org.apache.mahout.classifier.OnlineLearner;
import org.apache.mahout.classifier.VectorClassifier;

/**
 * Encodes the minimal common characteristics of a learning algorithm that can be tuned
 * by an evolutionary process.
 */
public interface AdjustableOnlineLearner extends OnlineLearner, VectorClassifier, Writable {
  AdjustableOnlineLearner lambda(double lambda);

  AdjustableOnlineLearner learningRate(double learningRate);

  AdjustableOnlineLearner decayExponent(double x);

  AdjustableOnlineLearner stepOffset(int offset);

  AdjustableOnlineLearner alpha(double alpha);

  int numCategories();

  int numFeatures();

  boolean validModel();

  PriorFunction getPrior();

  void copyFrom(AdjustableOnlineLearner model);
}
