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

package org.apache.mahout.classifier.sgd;

import org.apache.hadoop.io.Writable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides the ability to store SGD model-related objects as binary files.
 */
public final class ModelSerializer {
  // static class ... don't instantiate
  private ModelSerializer() {
  }

  public static void writeBinary(String path, CrossFoldLearner model) throws IOException {
    DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
    try {
      PolymorphicWritable.write(out, model);
    } finally {
      out.close();
    }
  }

  public static void writeBinary(String path, OnlineLogisticRegression model) throws IOException {
    DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
    try {
      PolymorphicWritable.write(out, model);
    } finally {
      out.close();
    }
  }

  public static void writeBinary(String path, AdjustableOnlineLearner model) throws IOException {
    PolymorphicWritable.write(new DataOutputStream(new FileOutputStream(path)), model);
  }

  public static <T extends Writable> T readBinary(InputStream in, Class<T> clazz) throws IOException {
    return PolymorphicWritable.read(new DataInputStream(in), clazz);
  }

  private static class PolymorphicTypeAdapter<T> implements JsonDeserializer<T>, JsonSerializer<T> {
    @Override
    public T deserialize(JsonElement jsonElement,
                                     Type type,
                                     JsonDeserializationContext jsonDeserializationContext) {
      JsonObject x = jsonElement.getAsJsonObject();
      try {
        //noinspection RedundantTypeArguments
        return jsonDeserializationContext.<T>deserialize(x.get("value"), Class.forName(x.get("class").getAsString()));
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException("Can't understand serialized data, found bad type: "
            + x.get("class").getAsString());
      }
    }

    @Override
    public JsonElement serialize(T x,
                                 Type type,
                                 JsonSerializationContext jsonSerializationContext) {
      JsonObject r = new JsonObject();
      r.add("class", new JsonPrimitive(x.getClass().getName()));
      r.add("value", jsonSerializationContext.serialize(x));
      return r;
    }
  }

  private static class CrossFoldLearnerTypeAdapter implements JsonDeserializer<CrossFoldLearner> {
    @Override
    public CrossFoldLearner deserialize(JsonElement jsonElement,
                                        Type type,
                                        JsonDeserializationContext jsonDeserializationContext) {
      CrossFoldLearner r = new CrossFoldLearner();
      JsonObject x = jsonElement.getAsJsonObject();
      r.setRecord(x.get("record").getAsInt());
      r.setAucEvaluator(jsonDeserializationContext.<OnlineAuc>deserialize(x.get("auc"), OnlineAuc.class));
      r.setLogLikelihood(x.get("logLikelihood").getAsDouble());

      JsonArray models = x.get("models").getAsJsonArray();
      for (JsonElement model : models) {
        r.addModel(
            jsonDeserializationContext.<OnlineLogisticRegression>deserialize(model, AdjustableOnlineLearner.class));
      }

      r.setParameters(asArray(x, "parameters"));
      r.setNumFeatures(x.get("numFeatures").getAsInt());
      r.setPrior(jsonDeserializationContext.<PriorFunction>deserialize(x.get("prior"), PriorFunction.class));
      return r;
    }
  }

  private static class AdaptiveLogisticRegressionTypeAdapter implements JsonSerializer<AdaptiveLogisticRegression>,
    JsonDeserializer<AdaptiveLogisticRegression> {

    @Override
    public AdaptiveLogisticRegression deserialize(JsonElement element, Type type, JsonDeserializationContext jdc) {
      JsonObject x = element.getAsJsonObject();
      AdaptiveLogisticRegression r =
          new AdaptiveLogisticRegression(x.get("numCategories").getAsInt(),
                                         x.get("numFeatures").getAsInt(),
                                         jdc.<PriorFunction>deserialize(x.get("prior"), PriorFunction.class));
      Type stateType = new TypeToken<State<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>() {}.getType();
      if (x.get("evaluationInterval")!=null) {
        r.setInterval(x.get("evaluationInterval").getAsInt());
      } else {
        r.setInterval(x.get("minInterval").getAsInt(), x.get("minInterval").getAsInt());
      }
      r.setRecord(x.get("record").getAsInt());

      Type epType = new TypeToken<EvolutionaryProcess<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>() {}.getType();
      r.setEp(jdc.<EvolutionaryProcess<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>deserialize(x.get("ep"), epType));
      r.setSeed(jdc.<State<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>deserialize(x.get("seed"), stateType));
      if (x.get("best") != null) {
        r.setBest(jdc.<State<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>deserialize(x.get("best"), stateType));
      }

      if (x.get("buffer") != null) {
        r.setBuffer(jdc.<List<AdaptiveLogisticRegression.TrainingExample>>deserialize(x.get("buffer"),
          new TypeToken<List<AdaptiveLogisticRegression.TrainingExample>>() {
          }.getType()));
      }
      return r;
    }

    @Override
    public JsonElement serialize(AdaptiveLogisticRegression x, Type type, JsonSerializationContext jsc) {
      JsonObject r = new JsonObject();
      r.add("ep", jsc.serialize(x.getEp(),
          new TypeToken<EvolutionaryProcess<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>() {}.getType()));
      r.add("minInterval", jsc.serialize(x.getMinInterval()));
      r.add("maxInterval", jsc.serialize(x.getMaxInterval()));
      Type stateType = new TypeToken<State<AdaptiveLogisticRegression.Wrapper, CrossFoldLearner>>() {}.getType();
      r.add("best", jsc.serialize(x.getBest(), stateType));
      r.add("numFeatures", jsc.serialize(x.getNumFeatures()));
      r.add("numCategories", jsc.serialize(x.getNumCategories()));
      PriorFunction prior = x.getPrior();
      JsonElement pf = jsc.serialize(prior, PriorFunction.class);
      r.add("prior", pf);
      r.add("record", jsc.serialize(x.getRecord()));
      r.add("seed", jsc.serialize(x.getSeed(), stateType));
      return r;
    }
  }

  /**
   * Tells GSON how to (de)serialize a Mahout matrix.  We assume on deserialization that the matrix
   * is dense.
   */
  private static class MatrixTypeAdapter
    implements JsonDeserializer<Matrix>, JsonSerializer<Matrix>, InstanceCreator<Matrix> {
    @Override
    public JsonElement serialize(Matrix m, Type type, JsonSerializationContext jsonSerializationContext) {
      JsonObject r = new JsonObject();
      r.add("rows", new JsonPrimitive(m.numRows()));
      r.add("cols", new JsonPrimitive(m.numCols()));
      JsonArray v = new JsonArray();
      for (int row = 0; row < m.numRows(); row++) {
        JsonArray rowData = new JsonArray();
        for (int col = 0; col < m.numCols(); col++) {
          rowData.add(new JsonPrimitive(m.get(row, col)));
        }
        v.add(rowData);
      }
      r.add("data", v);
      return r;
    }

    @Override
    public Matrix deserialize(JsonElement x, Type type, JsonDeserializationContext jsonDeserializationContext) {
      JsonObject data = x.getAsJsonObject();
      Matrix r = new DenseMatrix(data.get("rows").getAsInt(), data.get("cols").getAsInt());
      int i = 0;
      for (JsonElement row : data.get("data").getAsJsonArray()) {
        int j = 0;
        for (JsonElement element : row.getAsJsonArray()) {
          r.set(i, j, element.getAsDouble());
          j++;
        }
        i++;
      }
      return r;
    }

    @Override
    public Matrix createInstance(Type type) {
      return new DenseMatrix();
    }
  }


  /**
   * Tells GSON how to (de)serialize a Mahout matrix.  We assume on deserialization that the
   * matrix is dense.
   */
  private static class VectorTypeAdapter
    implements JsonDeserializer<Vector>, JsonSerializer<Vector>, InstanceCreator<Vector> {
    @Override
    public JsonElement serialize(Vector m, Type type, JsonSerializationContext jsonSerializationContext) {
      JsonObject r = new JsonObject();
      JsonArray v = new JsonArray();
      for (int i = 0; i < m.size(); i++) {
        v.add(new JsonPrimitive(m.get(i)));
      }
      r.add("data", v);
      return r;
    }

    @Override
    public Vector deserialize(JsonElement x, Type type, JsonDeserializationContext jsonDeserializationContext) {
      JsonArray data = x.getAsJsonObject().get("data").getAsJsonArray();
      Vector r = new DenseVector(data.size());
      int i = 0;
      for (JsonElement v : data) {
        r.set(i, v.getAsDouble());
        i++;
      }
      return r;
    }

    @Override
    public Vector createInstance(Type type) {
      return new DenseVector();
>>>>>>> Made ALR and CFL be polymorphic by using the new AdjustableOnlineLearner
    }
  }

  public static void writeBinary(String path, AdaptiveLogisticRegression model) throws IOException {
    DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
    try {
      PolymorphicWritable.write(out, model);
    } finally {
      out.close();
    }
  }

  public static <T extends Writable> T readBinary(InputStream in, Class<T> clazz) throws IOException {
    DataInputStream dataIn = new DataInputStream(in);
    try {
      return PolymorphicWritable.read(dataIn, clazz);
    } finally {
      dataIn.close();
    }
  }


}
