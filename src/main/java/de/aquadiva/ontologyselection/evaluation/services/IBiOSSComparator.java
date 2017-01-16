package de.aquadiva.ontologyselection.evaluation.services;

import java.util.List;

import de.aquadiva.ontologyselection.base.data.OntologySet;
import de.aquadiva.ontologyselection.base.data.ScoreType;
import de.aquadiva.ontologyselection.evaluation.data.BiOSSParameters;
import de.aquadiva.ontologyselection.evaluation.data.EvaluationSetting;
import de.aquadiva.ontologyselection.processes.services.OntologyModuleSelectionService.SelectionParameters;

public interface IBiOSSComparator {
	 void run(List<EvaluationSetting> evalSettings, ScoreType[] scoreTypesToConsider, String outputFile, SelectionParameters params, BiOSSParameters biossParameters, String settingsFileName);
	 List<OntologySet> getADOSResults(String inputTerms, SelectionParameters params);
}
