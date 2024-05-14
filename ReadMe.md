# KEML Analysis

This project analyses KEML files statistically. For each KEML file it produces:
1) [General Statistics](#general-statistics)
2) [Argumentation Statistics](#argumentation-statistics)
3) [Trust Scores](#trust-scores)

## Running

This project is a basic java application you can run in all normal ways (command line, IDE...).
It has one optional input: the base folder. If none is given, it creates statistics on the complete example from keml.sample - assuming that project is located on the same level as keml.sample.
All these files are stored in the folder **analysis**.


## Output
In **analysis**, each filename starts with a prefix $pre$ that is equal to the KEML file name.

### General Statistics
General statistics are stored under $pre$-general.csv.

This CSV file holds a Message Part and a Knowledge Part where it gives statistics per Conversation Partner. 
The Message Part gives counts for sends and receives, as well as interruptions.
The Knowledge Part counts PreKnowledge and New information, split into Facts and Instructions. It also counts repetitions.


### Argumentation Statistics
Argumentation statistics are stored under $pre$-arguments.csv.



### Trust Scores


## License
The license of this project is that of the [group](https://gitlab.uni-koblenz.de/keml).