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

import java.util.concurrent.TimeUnit;
import org.apache.lucene.util.ArrayUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BM25VectorizationBenchmark {

  @Param({"128", "512", "1024", "4096"})
  private int size;

  private float[] freqs;
  private long[] norms;
  private float[] scores;
  private float[] cache;
  private float weight;

  // Temporary arrays for multi-loop approach
  private float[] normInverses;
  private float[] tempScores;

  @Setup
  public void setup() {
    freqs = new float[size];
    norms = new long[size];
    scores = new float[size];
    cache = new float[256];
    weight = 2.5f;

    // Initialize test data
    for (int i = 0; i < size; i++) {
      freqs[i] = 1.0f + (i % 100) * 0.1f;
      norms[i] = i % 256;
    }

    // Initialize BM25 cache (typical values)
    float k1 = 1.2f;
    float b = 0.75f;
    float avgdl = 100.0f;
    for (int i = 0; i < 256; i++) {
      float dl = i * 4.0f; // Approximate document length
      cache[i] = 1f / (k1 * ((1 - b) + b * dl / avgdl));
    }

    // Pre-allocate arrays for multi-loop approach
    normInverses = new float[size];
    tempScores = new float[size];
  }

  @Benchmark
  public void singleLoop() {
    for (int i = 0; i < size; i++) {
      float normInverse = cache[((byte) norms[i]) & 0xFF];
      scores[i] = weight - weight / (1f + freqs[i] * normInverse);
    }
  }

  @Benchmark
  public void multiLoop() {
    // Loop 1: Vectorizable norm lookup
    for (int i = 0; i < size; i++) {
      normInverses[i] = cache[((byte) norms[i]) & 0xFF];
    }

    // Loop 2: Vectorizable multiplication
    for (int i = 0; i < size; i++) {
      tempScores[i] = freqs[i] * normInverses[i];
    }

    // Loop 3: Vectorizable arithmetic
    for (int i = 0; i < size; i++) {
      scores[i] = weight - weight / (1f + tempScores[i]);
    }
  }

  @Benchmark
  public void multiLoopWithRealloc() {
    // Simulate the actual BM25 implementation with array reallocation
    if (normInverses.length < size) {
      normInverses = new float[ArrayUtil.oversize(size, Float.BYTES)];
      tempScores = new float[ArrayUtil.oversize(size, Float.BYTES)];
    }

    // Loop 1: Vectorizable norm lookup
    for (int i = 0; i < size; i++) {
      normInverses[i] = cache[((byte) norms[i]) & 0xFF];
    }

    // Loop 2: Vectorizable multiplication
    for (int i = 0; i < size; i++) {
      tempScores[i] = freqs[i] * normInverses[i];
    }

    // Loop 3: Vectorizable arithmetic
    for (int i = 0; i < size; i++) {
      scores[i] = weight - weight / (1f + tempScores[i]);
    }
  }
}