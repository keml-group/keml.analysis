# KEML Analysis

**Note:** This branch features an alternative [KEML](https://github.com/keml-group/) component that leverages logic-based argumentation framework (**LAF**), and is designed *only* to be used along other LAF components of KEML. For the corresponding baseline version of this component, see the baseline [bipolar weighted framework](https://github.com/keml-group/keml.analysis) version.
-----------------------
This project analyses KEML files statistically. For each KEML file it produces:
1) [General Statistics](#general-statistics)
2) [Argumentation data](#argumentation-data)
3) [Trust Scores](#trust-scores)

## Installation

Since this project uses EMF components, it is best viewed and adapted from Eclipse. If you load it there, make sure that you have the right project natures, that is **modeling** and **maven**.
If you freshly added maven to this project in Eclipse, it might be necessary to run Maven -> Update project on it before using maven to install the necessary libraries.

## Running

This project is a basic maven based java application you can run in all normal ways (command line, IDE...).
It has one optional input: the base folder. If none is given, it creates statistics on the LAF examples from the LAF branch of keml.sample - assuming that project is located on the same level as keml.sample.
All output files are stored in the folder **analysis**.

## Output
In **analysis**, each filename starts with a prefix _pre_ that is equal to the KEML file name.

Currently, three types of statistics are generated:
1) [General Statistics](#general-statistics)
2) [Argumentation data](#argumentation-data)
3) [Trust Scores](#trust-scores)

### General Statistics
General statistics are stored under $pre$-general.csv.

This CSV file holds a Message Part and a Knowledge Part where it gives statistics per Conversation Partner. 
The Message Part gives counts for sends and receives, as well as interruptions.
The Knowledge Part counts PreKnowledge and New information, split into Facts and Instructions. It also counts repetitions.

![Example General Statistics](doc/laf_example-general-csv.png)


### Argumentation Data
Argumentation data are stored under _pre_-arguments.csv.

This CSV file consists of multiple relevant data to the state of the analysied converastion:
- A mapping of every modeled piece of information and a unique literal-symbol assigned to it (e.g., "L1").
- A list of all derived logic arguments for every literal/information piece.
- A String-representation of the constructed undercut trees.
- A list of all rebuttals found for each literal/information piece


![Example Argumentation Info and Literal Mapping](doc/laf_example-arguments-output1.png)
![Example Derived Logic Arguments](doc/laf_example-arguments-output2.png)
![Example Constructed Undercut Trees](doc/laf_example-arguments-undercuts.png)
![Example Found Rebuttals](doc/laf_example-arguments-rebuttals.png)

### Trust Scores

Trust Scores are given as Excel (xlsx) files _pre_-scores-trust.xlsx. As opposed to the baseline version which uses initial trust to model the influence of attacks and supports on information pieces, the approach of this LAF-version leverages the count of arguments for and against a given claim using categorizer and accumulator functions on argumentation structures in logic-based argumentation as introduced and discussed in the paper [A logic-based theory of deductive theory](https://doi.org/10.1016/S0004-3702%2801%2900071-6) by P. Besnard and A. Hunter. The goal is to model trust in a given based on how many arguments can be made for it or against it.

The .xlsx file showcases [categorization](#categorization) and [accumulation](#accumulation) values for each information, considering the logical arguments that could be derived for it and against it, the logical arguments that undercut it, and the logical arguments that rebut it.
Additionally the file depicts the following data:
1) The **time stamp** (-1 for pre knowledge) with the background color stating whether i is fact (green) or instruction (orange)
2) The **message** column with the background color blue for LLM messages and yellow for all other messages
3) The **argument count \#Arg+** counting how many arguments support the information
4) The **argument count \#Arg-** counting how many arguments attack the information

![Example Trust Scores](doc/laf_example-trust-xlsx.png)


#### Categorization
The goal is to assign a numerical value to argument trees based on the amount of arguments that attack it (i.e., children), attackers of attackers (i.e., children of children) and so on recursively.
A specific version of this function is used to that end, namely the hCategorizer:

$h(N) = \frac{1}{1+ h(N_1)+...+ h(N_l)}$, where $N$ is the root argument and $N_1,..., N_l$ are children of the $N$  

In the .xlsx file we use hCat+ to refer to the categorization values of arguments for a given claim, and hCat- for the categorization values of arguments against a given claim.

#### Accumulation

Using categorization values assigned to arguments for and against a given claim, an accumulator function aggregates these values to compute a balance. A specific accumulator function is used, namely the logAccumulator:
$logAccu(X,Y) = log(1 + X_1 + ... + X_l) - log(1 + Y_1 + ... + Y_l)$, where $X_1,...,X_l$ is the list of categorized arguments for a claim and $Y_1,...,Y_l$ are that of arguments against the claim.

The resulting accumulation value can be interpreted as follows:
- $logAccu(X,Y) > 0$ : indicates that the arguments for the claim in question are stronger. The higher the value, the more trustworthy the claim is on basis of the count of unchallenged/equally challenged arguments for it.
- $logAccu(X,Y) = 0$ : indicates a neutral claim.
- $logAccu(X,Y) < 0$ indicates that the arguments against the claim in question are stronger. The lower the value, the less trustworthy the claim is on basis of the count of unchallenged/equally challenged arguments against it.

## License
The license of this project is that of the [group](https://github.com/keml-group).
