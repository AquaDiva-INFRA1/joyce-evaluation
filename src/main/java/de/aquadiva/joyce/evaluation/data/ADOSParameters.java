package de.aquadiva.joyce.evaluation.data;

import de.aquadiva.joyce.processes.services.SelectionParameters;

public class ADOSParameters {
	public SelectionParameters.SelectionType selection_object;
	public boolean consider_coverage;
	public boolean consider_overhead;
	public boolean consider_overlap;
	public int preference_coverage;
	public int preference_overhead;
	public int preference_overlap;
	public int maximum_number_of_iterations;
	public int sample_size;
	public String ontologies;
}
