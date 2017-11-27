# JOYCE Evaluation

For a general overview over the JOYCE project and instructions how to install and run JOYCE, please refer to the README.md file of the joyce repository. However note that the evaluation has its own applications to run on a built JOYCE repository.

This is the evaluation project to test the coverage / overhead / overlap of selected ontologies or ontology modules by JOYCE vs. the NCBO Recommender.
The main evaluation class is `de.aquadiva.joyce.evaluation.JoyceEval`. It reads configuration files with sets of parameters that are tested automatically. These settings are modeled by the `de.aquadiva.joyce.evaluation.data.EvaluationSetting` and `de.aquadiva.joyce.evaluation.data.Setting` classes. To get a better understanding of how the evaluation works, the best thing is to dig into the code.
 
