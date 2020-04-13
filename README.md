ADMM4J: Alternating Direction Method of Multipliers for Java
==============================================================

ADMM4J is an open source Java library for addressing challenging optimization problems using decomposition modeling and solving. 
It is an implementation of Alternating Direction Method of Multipliers (ADMM). 
ADMM is suitable for solving complex problems by decomposition where the problem structure is represented by a bipartite graph. 
The nodes in the graph are associated with local subproblems that are solved in parallel.
ADMM4J is designed to be able to run in parallel and distributed settings. 
Currently, only a multi-threaded implementation is available.  In the future, a version for distributed setting will be added.


## Build and Run

Use [Maven](https://maven.apache.org/). First build admm4j-core. Next build admm4j-demo.

```
mvn clean install
```


To cite this repository in publications:

      @misc{admm4j,
        author = {Denysiuk, Roman},
        title = {ADMM4J: Alternating Direction Method of Multipliers for Java},
        journal = {GitHub repository},
        year = {2020},
        publisher = {GitHub},  
        howpublished = {\url{https://github.com/romden/admm4j}},
      }
