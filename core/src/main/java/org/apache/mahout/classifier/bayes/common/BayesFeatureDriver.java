package org.apache.mahout.classifier.bayes.common;
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

import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.KeyValueTextInputFormat;
import org.apache.hadoop.io.DefaultStringifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;


/**
 * Create and run the Bayes Feature Reader Step.
 *
 **/
public class BayesFeatureDriver {
  /**
   * Takes in two arguments:
   * <ol>
   * <li>The input {@link org.apache.hadoop.fs.Path} where the input documents live</li>
   * <li>The output {@link org.apache.hadoop.fs.Path} where to write the interim files as a {@link org.apache.hadoop.io.SequenceFile}</li>
   * </ol>
   * @param args The args
   */
  public static void main(String[] args) {
    String input = args[0];
    String output = args[1];

    runJob(input, output, 1);
  }

  /**
   * Run the job
   *
   * @param input            the input pathname String
   * @param output           the output pathname String

   */
  
  @SuppressWarnings("deprecation")
  public static void runJob(String input, String output, int gramSize) {
    JobClient client = new JobClient();
    JobConf conf = new JobConf(BayesFeatureDriver.class);
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(FloatWritable.class);
    
    conf.setInputPath(new Path(input));
    Path outPath = new Path(output);
    conf.setOutputPath(outPath);
    conf.setNumMapTasks(100);
    //conf.setNumReduceTasks(1);
    conf.setMapperClass(BayesFeatureMapper.class);

    conf.setInputFormat(KeyValueTextInputFormat.class);
    conf.setCombinerClass(BayesFeatureReducer.class);
    conf.setReducerClass(BayesFeatureReducer.class);    
    conf.setOutputFormat(BayesFeatureOutputFormat.class);

    conf.set("io.serializations", "org.apache.hadoop.io.serializer.JavaSerialization,org.apache.hadoop.io.serializer.WritableSerialization"); // Dont ever forget this. People should keep track of how hadoop conf parameters and make or break a piece of code
    
    try {
      FileSystem dfs = FileSystem.get(conf);
      if (dfs.exists(outPath))
        dfs.delete(outPath, true);
      
      DefaultStringifier<Integer> intStringifier = new DefaultStringifier<Integer>(conf, Integer.class);     
      String gramSizeString = intStringifier.toString(new Integer(gramSize));
      
      Integer retGramSize = intStringifier.fromString(gramSizeString);      
      System.out.println(retGramSize);
      conf.set("bayes.gramSize", gramSizeString);
      
      client.setConf(conf);    
      JobClient.runJob(conf);      
      
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
  }
}