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
package org.apache.lucene.benchmark.jmh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.store.OutputStreamIndexOutput;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(value = 2)
public class DataOutputBulkAPIBenchmark {

  final int maxSize = 256;
  final int[] values = new int[maxSize];

  ByteArrayOutputStream baos;
  OutputStreamIndexOutput outputStreamOut;

  @Param({"16", "32", "64", "128", "256"})
  public int size;

  @Setup(Level.Trial)
  public void init() {
    Random r = new Random(0);
    for (int i = 0; i < maxSize; ++i) {
      values[i] = r.nextInt();
    }
    baos = new ByteArrayOutputStream();
    outputStreamOut = new OutputStreamIndexOutput("test", "test", baos, 8192);
  }

  @Benchmark
  public void benchOutputStreamIndexOutput_writeInt() throws IOException {
    baos.reset();
    for (int i = 0; i < size; i++) {
      outputStreamOut.writeInt(values[i]);
    }
  }

  @Benchmark
  public void benchOutputStreamIndexOutput_writeInts() throws IOException {
    baos.reset();
    outputStreamOut.writeInts(values, 0, size);
  }
}
