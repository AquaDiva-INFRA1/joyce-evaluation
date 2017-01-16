package de.aquadiva.ontologyselection.evaluation.services;

import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;

import de.aquadiva.ontologyselection.processes.services.OSProcessesModule;

@SubModule(OSProcessesModule.class)
public class OSEvaluationModule {
	public static void bind(ServiceBinder binder) {
		binder.bind(IBiOSSComparator.class, BiOSSComparator.class);
	}
}
