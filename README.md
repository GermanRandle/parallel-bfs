# parallel-bfs

Comparing sequential and parallel implementations of BFS algorithm.

The source code for both algorithms is [here](https://github.com/GermanRandle/parallel-bfs/blob/main/app/src/main/kotlin/german/randle/bfs/BfsAlgorithms.kt).
Tests are [here](https://github.com/GermanRandle/parallel-bfs/blob/main/app/src/test/kotlin/german/randle/bfs/BfsTest.kt).

### How to run

1) Install Gradle: https://gradle.org/install/

2) Execute in terminal:
```
parallel-bfs % ./gradlew run
```

### Results on my local machine

```declarative
LAUNCH #1
SEQUENTIAL TIME: 22028 ms
PARALLEL TIME: 6435 ms
RATIO: 3.4231546231546233
LAUNCH #2
SEQUENTIAL TIME: 23292 ms
PARALLEL TIME: 6146 ms
RATIO: 3.7897819720143184
LAUNCH #3
SEQUENTIAL TIME: 22103 ms
PARALLEL TIME: 6067 ms
RATIO: 3.6431514751936707
LAUNCH #4
SEQUENTIAL TIME: 21780 ms
PARALLEL TIME: 6039 ms
RATIO: 3.6065573770491803
LAUNCH #5
SEQUENTIAL TIME: 21752 ms
PARALLEL TIME: 6188 ms
RATIO: 3.51519069166128

FINAL RESULTS:
SEQUENTIAL TIME: 22191.0 ms
PARALLEL TIME: 6175.0 ms
RATIO: 3.5936842105263156
```
