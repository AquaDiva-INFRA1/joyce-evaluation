package de.aquadiva.ontologyselection.evaluation.bioss.data.bioportal;

import java.util.List;

import de.aquadiva.ontologyselection.base.data.bioportal.OntologyInformation;

public class RecommendationResult {
	public double evaluationScore;
	public List<OntologyInformation> ontologies;
	public CoverageResult coverageResult;
	public SpecializationResult specializationResult;
	public AcceptanceResult acceptanceResult;
	public DetailResult detailResult;
}
