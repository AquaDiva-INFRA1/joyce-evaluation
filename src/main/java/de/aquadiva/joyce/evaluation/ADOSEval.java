package de.aquadiva.joyce.evaluation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.aquadiva.joyce.JoyceSymbolConstants;
import de.aquadiva.joyce.base.data.Ontology;
import de.aquadiva.joyce.base.data.ScoreType;
import de.aquadiva.joyce.base.util.ErrorFromNCBORecommenderException;
import de.aquadiva.joyce.evaluation.data.Setting;
import de.aquadiva.joyce.evaluation.services.IBiOSSComparator;
import de.aquadiva.joyce.evaluation.services.JoyceEvaluationModule;
import de.aquadiva.joyce.processes.services.OntologyModuleSelectionService;
import de.aquadiva.joyce.processes.services.OntologyModuleSelectionService.SelectionParameters;

public class ADOSEval {

	private static final Logger log = LoggerFactory.getLogger(ADOSEval.class);
	
	public static void main(String[] args) throws ErrorFromNCBORecommenderException {

		Registry registry = null;
		try {
			registry = RegistryBuilder.buildAndStartupRegistry(JoyceEvaluationModule.class);
			IBiOSSComparator biosComparator = registry.getService(IBiOSSComparator.class);

			String settingsDir = "eval/settings/";
			String reportFilename = "results-summary-settings.csv";

			// get all files in the settings folder and run the tool for each
			// settings-file
			List<String> fileList = fileList(settingsDir);
			for (String settingsFileName : fileList) {
				// String settingsFileName = "eval/settings.json";
				Setting setting = readSettingsFile(settingsFileName);
				SelectionParameters adosParams = getADOSConfigurationParameters(setting);
				String apiKey = System.getProperty(JoyceSymbolConstants.BIOPORTAL_API_KEY);
				biosComparator.run(setting.evaluation_settings, adosParams.scoreTypesToConsider, reportFilename,
						adosParams, setting.BiOSS_parameters, settingsFileName, apiKey);
			}

			// String reportFilename = getExperimentFileName(adosParams);

		} finally {
			log.info("Evaluation finished.");
			if (null != registry) {
//				// TODO: That's not good, all the services should be registered
//				// to the registry's shutdown hub and execute their own
//				// shutdowns automatically
//				IOntologyDBService dbservice = registry.getService(IOntologyDBService.class);
//				dbservice.shutdown();
//				ExecutorService executorService = registry.getService(ExecutorService.class);
//				List<Runnable> remainingThreads = executorService.shutdownNow();
//				if (remainingThreads.size() != 0)
//					System.out.println("Wait for " + remainingThreads.size() + " to end.");
//				try {
//					executorService.awaitTermination(10, TimeUnit.MINUTES);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				registry.shutdown();
				log.info("Registry has been shut down.");
			}
		}

	}

	static List<String> fileList(String directory) {
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directory))) {
			for (Path path : directoryStream) {
				fileNames.add(path.toString());
			}
		} catch (IOException ex) {
		}
		return fileNames;
	}

	private static SelectionParameters getADOSConfigurationParameters(Setting setting) {
		SelectionParameters adosParams = new OntologyModuleSelectionService.SelectionParameters();

		adosParams.sampleSize = setting.ADOS_parameters.sample_size;
		adosParams.maxElementsPerSet = setting.ADOS_parameters.maximum_number_of_iterations;
		adosParams.selectionType = setting.ADOS_parameters.selection_object;

		// set criteria to consider and their preferences
		ArrayList<ScoreType> criteriaList = new ArrayList<ScoreType>();
		ArrayList<Integer> preferencesList = new ArrayList<Integer>();
		if (setting.ADOS_parameters.consider_coverage) {
			criteriaList.add(ScoreType.TERM_COVERAGE);
			preferencesList.add(setting.ADOS_parameters.preference_coverage);
		}

		if (setting.ADOS_parameters.consider_overhead) {
			criteriaList.add(ScoreType.CLASS_OVERHEAD);
			preferencesList.add(setting.ADOS_parameters.preference_overhead);
		}

		if (setting.ADOS_parameters.consider_overlap) {
			criteriaList.add(ScoreType.CLASS_OVERLAP);
			preferencesList.add(setting.ADOS_parameters.preference_overlap);
		}

		adosParams.scoreTypesToConsider = criteriaList.toArray(new ScoreType[criteriaList.size()]);
		adosParams.preferences = preferencesList.toArray(new Integer[preferencesList.size()]);

		return adosParams;
	}

	private static Setting readSettingsFile(String settingsFileName) {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").create();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(settingsFileName));
			return gson.fromJson(br, Setting.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private static String getExperimentFileName(SelectionParameters params) {
		return "adosevalresults" + StringUtils.join(params.preferences) + "_sample" + params.sampleSize + "_step"
				+ params.maxElementsPerSet + "_" + params.selectionType.name().toLowerCase();
	}

	public static Set<String> getClassIrisInOntologies(Iterable<Ontology> ontologies) {
		List<OWLOntology> owlOntologies = new ArrayList<>();
		for (Ontology o : ontologies) {
			if (o.getOwlOntology() == null)
				throw new IllegalStateException("Ontology with ID " + o.getId()
						+ " does not have an OWLOntology set. For this method to work, a non-null OWLOntology is necessary.");
			owlOntologies.add(o.getOwlOntology());
		}
		return getClassIrisInOwlOntologies(owlOntologies);
	}

	private static Set<String> getClassIrisInOwlOntologies(Iterable<OWLOntology> owlOntologies) {
		Set<String> classIris = new HashSet<>();
		for (OWLOntology o : owlOntologies) {
			Set<OWLClass> classesInSignature = o.getClassesInSignature();
			for (OWLClass c : classesInSignature) {
				classIris.add(c.getIRI().toString());
			}
		}
		return classIris;
	}


}
