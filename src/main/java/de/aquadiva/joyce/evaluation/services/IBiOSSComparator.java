package de.aquadiva.joyce.evaluation.services;

import java.util.List;

import de.aquadiva.joyce.base.data.OntologySet;
import de.aquadiva.joyce.base.data.ScoreType;
import de.aquadiva.joyce.base.util.ErrorFromNCBORecommenderException;
import de.aquadiva.joyce.evaluation.data.BiOSSParameters;
import de.aquadiva.joyce.evaluation.data.EvaluationSetting;
import de.aquadiva.joyce.processes.services.SelectionParameters;

public interface IBiOSSComparator {
	 void run(List<EvaluationSetting> evalSettings, ScoreType[] scoreTypesToConsider, String outputFile, SelectionParameters params, BiOSSParameters biossParameters, String settingsFileName, String apiKey) throws ErrorFromNCBORecommenderException;
	 List<OntologySet> getADOSResults(String inputTerms, SelectionParameters params);
}
