package de.aquadiva.ontologyselection.evaluation.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;

import com.google.common.collect.Multiset;

import de.aquadiva.ontologyselection.base.data.IOntology;
import de.aquadiva.ontologyselection.base.data.IOntologySet;
import de.aquadiva.ontologyselection.base.data.Ontology;
import de.aquadiva.ontologyselection.base.data.OntologySet;
import de.aquadiva.ontologyselection.base.data.ScoreType;
import de.aquadiva.ontologyselection.base.data.bioportal.OntologyInformation;
import de.aquadiva.ontologyselection.base.services.IConstantOntologyScorer;
import de.aquadiva.ontologyselection.base.services.IOntologyDBService;
import de.aquadiva.ontologyselection.base.services.IVariableOntologyScorer;
import de.aquadiva.ontologyselection.base.services.OWLParsingService;
import de.aquadiva.ontologyselection.core.services.IConceptTaggingService;
import de.aquadiva.ontologyselection.evaluation.ADOSEval;
import de.aquadiva.ontologyselection.evaluation.bioss.BiOSSConnector;
import de.aquadiva.ontologyselection.evaluation.bioss.data.bioportal.RecommendationResult;
import de.aquadiva.ontologyselection.evaluation.data.BiOSSParameters;
import de.aquadiva.ontologyselection.evaluation.data.EvaluationSetting;
import de.aquadiva.ontologyselection.processes.services.ConstantScoringChain;
import de.aquadiva.ontologyselection.processes.services.IOntologyModuleSelectionService;
import de.aquadiva.ontologyselection.processes.services.OntologyModuleSelectionService.SelectionParameters;
import de.aquadiva.ontologyselection.processes.services.VariableScoringChain;
import de.aquadiva.ontologyselection.util.NoResultFromNCBORecommenderException;

public class BiOSSComparator implements IBiOSSComparator {
	private IOntologyDBService dbService;
	private Logger log;
	private IVariableOntologyScorer variableScoringChain;
	private IConstantOntologyScorer constantScoringChain;
	private IConceptTaggingService taggingService;
	public static String pathToInputOutputFolder = "../ad-ontology-selection-processes/selection_terms/";
	private IOntologyModuleSelectionService selectionService;

	public BiOSSComparator(IOntologyDBService dbService, Logger log,
			@VariableScoringChain IVariableOntologyScorer variableScoringChain,
			@ConstantScoringChain IConstantOntologyScorer constantScoringChain, IConceptTaggingService taggingService,
			IOntologyModuleSelectionService selectionService) {
		this.dbService = dbService;
		this.log = log;
		this.variableScoringChain = variableScoringChain;
		this.constantScoringChain = constantScoringChain;
		this.taggingService = taggingService;
		this.selectionService = selectionService;
	}

	public String retrieveResults() {
		return "retrieveResults was called ...";
	}
	
	public void run(List<EvaluationSetting> evalSettings, ScoreType[] scoreTypesToConsider, String outputFile,
			SelectionParameters params, BiOSSParameters biossParams, String settingsFileName) {
		// pathToInputOutputFolder = basefolder;
		// // parse settings file
		// ArrayList<String[]> settings = new ArrayList<String[]>();
		// BufferedReader in = null;
		// try {
		// in = new BufferedReader(new FileReader(pathToInputOutputFolder +
		// settingsFile));
		// String line = null;
		// while ((line = in.readLine()) != null) {
		// String[] lineContent = line.split("\t");
		// settings.add(lineContent);
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// if (in != null)
		// try {
		// in.close();
		// } catch (IOException e) {
		// }
		// }

		// iterate over the settings given in the evaluation settings
		int settingId = 0;
		for (EvaluationSetting setting : evalSettings) {

			// set base folder
			pathToInputOutputFolder = setting.base_folder;

			// get setting identifier
			// String settingIdentificator = generateSettingId(setting, params,
			// biossParams) + settingId;
			String settingIdentificator = "";
			int index = settingsFileName.lastIndexOf("/");
			if (index == -1) {
				settingIdentificator = settingsFileName.replace(".json", "") + settingId;
			} else {
				settingIdentificator = settingsFileName.substring(index + 1).replace(".json", "") + settingId;
			}

			// get BiOSS/ADOS results
			List<OntologySet> results = null;
			long startTime;
			long endTime;

			// get results
			if (setting.evaluate_BiOSS) {
				startTime = System.currentTimeMillis();
				results = getBiOSSResults(setting.input_terms, biossParams);
				endTime = System.currentTimeMillis();
			} else {
				startTime = System.currentTimeMillis();
				results = getADOSResults(setting.input_terms, params);
				endTime = System.currentTimeMillis();
			}

			// if the results file does not exist yet, create it and add a
			// header line
			Path resultsFile = Paths.get(pathToInputOutputFolder + outputFile);
			if (!Files.exists(resultsFile)) {
				try {
					Files.createFile(resultsFile);
					String header = "setting id\t";
					header += "top\t";
					for (ScoreType t : scoreTypesToConsider) {
						header += t.toString() + "\t";
					}
					header += "num results\t";
					header += "exec time\t";
					header += "av num of modules\t";
					header += "av module size";
					header += "\n";

					Files.write(resultsFile, header.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// run several metrics
			for (int avOver : setting.average_scores_over_top) {
				log.info("iterator item avOver: {}", avOver);

				// create output string
				String resultLine = settingIdentificator + "\t";

				resultLine += avOver + "\t";

				for (ScoreType t : scoreTypesToConsider) {
					resultLine += takeAverageOver(avOver, results, t) + "\t";
				}

				resultLine += results.size() + "\t";

				resultLine += (endTime - startTime) + "\t";

				resultLine += averageResultSetSize(avOver, results) + "\t";

				resultLine += averageModuleSize(avOver, results);

				log.info("result: {}", resultLine);

				// append output to file
				Writer output;
				try {
					output = new BufferedWriter(new FileWriter(pathToInputOutputFolder + outputFile, true));
					output.append(resultLine + "\n");
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				// create a directory per evaluation setting
				log.info("Creating a directory for evaluation setting " + settingIdentificator);
				Path dir = Paths.get(pathToInputOutputFolder, "setting-" + settingIdentificator);
				log.info("\tdirectory path: " + dir.getFileName());
				if (!Files.exists(dir))
					Files.createDirectory(dir);

				// write detailed results in separate files
				log.info("Iterating over the results");
				for (OntologySet s : results) {

					int numOfResults = 0;

					// create a directory per result
					log.info("\tcreating a directory for result " + numOfResults);
					Path dirRes = Paths.get(pathToInputOutputFolder, "setting-" + settingIdentificator,
							"result" + numOfResults);
					log.info("\t\tdirectory path: " + dirRes.getFileName());
					if (!Files.exists(dirRes))
						Files.createDirectory(dirRes);

					List<String> resultInfo = new ArrayList<String>();
					resultInfo.add("ontology id\tcoverage\toverhead\toverlap");

					HashSet<String> classIds = new HashSet<String>();
					
					// for each ontology/module
					log.info("\tIterating over the ontologies of this result");
					HashSet<IOntology> ontologies = s.getOntologies();
					for (IOntology o : ontologies) {

						log.info("\t\tProcessing ontology " + o.getId());

						// store the owl-file
						String fileName = dirRes.toString() + "/" + o.getId().toString().replace("temp/", "");
						log.info("\t\tstoring the owl-file " + fileName + ".owl");
						log.info("\t\tdata to store " + o.getOntologyData());
						Path owlFilePath = Paths.get(fileName + ".owl");

						byte[] owl = null;
						// write gzipped data to file
						if (isGZipCompressed(o.getOntologyData())) {
							// Files.write(owlFilePath, getGUnzippedBytes(
							// o.getOntologyData() ));
							owl = IOUtils.toByteArray(new GZIPInputStream(new ByteArrayInputStream(o.getOntologyData())));
							Files.write(owlFilePath, owl);
							// write non-compressed data to file
						} else {
							owl = o.getOntologyData();
							Files.write(owlFilePath, owl);
						}

						// store the ids of the classes contained
						log.info("\t\tAdd class ids");
						classIds.addAll(o.getClassIds());

						// store score information
						log.info("\t\tstore scores " + o.getId() + "\t" + o.getScore(ScoreType.TERM_COVERAGE) + "\t"
								+ o.getScore(ScoreType.CLASS_OVERHEAD) + "\t" + o.getScore(ScoreType.CLASS_OVERLAP));
						resultInfo.add(o.getId() + "\t" + o.getScore(ScoreType.TERM_COVERAGE) + "\t"
								+ o.getScore(ScoreType.CLASS_OVERHEAD) + "\t" + o.getScore(ScoreType.CLASS_OVERLAP));

					}

					numOfResults++;
					
//					// write found class IRIs
//					Set<String> classIRIs =  ADOSEval.getClassIrisInOntologies(s.getOntologies());
//					Path classIRIPath = Paths.get(dirRes.toString(), "classIRIs.txt");
//					log.info("\twriting class IRIs in " + classIRIPath.getFileName());
//					Files.write(classIRIPath, classIRIs, Charset.forName("UTF-8"));

//					// write found class IRIs
//					Path classIRIPath = Paths.get(dirRes.toString(), "classIRIs.txt");
//					log.info("\twriting class IRIs in " + classIRIPath.getFileName());
//					Files.write(classIRIPath, classIRIs, Charset.forName("UTF-8"));
					
					// write summary information
					Path scoresPath = Paths.get(dirRes.toString(), "scores.csv");
					log.info("\twriting scores for this result in " + scoresPath.getFileName());
					Files.write(scoresPath, resultInfo, Charset.forName("UTF-8"));

					// write found class ids
					Path classIdPath = Paths.get(dirRes.toString(), "classIds.txt");
					log.info("\twriting class ids in " + classIdPath.getFileName());
					Files.write(classIdPath, classIds, Charset.forName("UTF-8"));

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			settingId++;
		}
	}

	private static String generateSettingId(EvaluationSetting setting, SelectionParameters params,
			BiOSSParameters biossParams) {
		String settingId = "";

		if (setting.evaluate_BiOSS) {
			settingId += "bioss-ontology-100-";
			settingId += biossParams.maximal_number_of_elements_per_set + "-1-";
		} else {
			settingId += "ados-";
			if (params.selectionType.toString().equals("CLUSTER_MODULE")) {
				settingId += "cluster-";
			} else if (params.selectionType.toString().equals("LOCALITY_MODULE")) {
				settingId += "locality-";
			} else {
				settingId += "ontology-";
			}
			settingId += params.preferences[0];
			settingId += params.preferences[1];
			settingId += params.preferences[2];
			settingId += "-";

			settingId += params.maxElementsPerSet + "-" + params.sampleSize + "-";
		}

		settingId += setting.input_terms.hashCode() + "-";

		return settingId;
	}

	/**
	 * Returns the average score of the given type in the given data over the
	 * first averageOver results. If there are less results than averageOver,
	 * null is returned.
	 * 
	 * @param averageOver
	 * @param data
	 * @param type
	 * @return
	 */
	private Double takeAverageOver(int averageOver, List<OntologySet> data, ScoreType type) {
		double sum = 0;
		for (int j = 0; j < averageOver; j++) {
			if (j < data.size()) {
				Double score = data.get(j).getScore(type);
				sum += score;
			} else {
				return null;
			}
		}

		double result = (double) sum / (double) averageOver;
		return result;
	}

	private Double averageResultSetSize(int averageOver, List<OntologySet> data) {
		double sum = 0;
		for (int j = 0; j < averageOver; j++) {
			if (j < data.size()) {
				Double size = Double.valueOf(data.get(j).getOntologies().size());
				sum += size;
			} else {
				return null;
			}
		}

		double result = (double) sum / (double) averageOver;
		return result;
	}

	private Double averageModuleSize(int averageOver, List<OntologySet> data) {
		double sum = 0;
		for (int j = 0; j < averageOver; j++) {
			if (j < data.size()) {
				HashSet<IOntology> resultSet = data.get(j).getOntologies();
				// determine average module size
				double sizeSum = 0;
				for (IOntology o : resultSet) {
					sizeSum += (double) o.getClassIds().size();
				}
				sum += (sizeSum / (double) resultSet.size());
			} else {
				return null;
			}
		}

		double result = (double) sum / (double) averageOver;
		return result;
	}

	/**
	 * Generates numberOfTermsetsPerSetting term sets of of all combinations of
	 * the given sizes with the given diversities and writes these to a file
	 * with the given name.
	 * 
	 * @param numberOfTermsetsPerSetting
	 * @param sizes
	 * @param diversities
	 * @param fileName
	 */
	public static void generateTestSettings(int numberOfTermsetsPerSetting, int[] sizes, int[] diversities,
			String fileName) {

		try {

			BufferedWriter out = new BufferedWriter(new FileWriter(pathToInputOutputFolder + fileName));

			int termsetId = 0;
			for (int s : sizes) {
				for (int d : diversities) {
					for (int i = 0; i < numberOfTermsetsPerSetting; i++) {
						String terms = generateInputTerms(s, d);
						out.write(termsetId + "\t" + terms + "\t" + s + "\t" + d);
						out.newLine();
						termsetId++;
					}
				}
			}

			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a term set of the given size and diversity.
	 * 
	 * @param size
	 * @param diversity
	 * @return
	 */
	private static String generateInputTerms(int size, int diversity) {
		String fileName = pathToInputOutputFolder;
		// get proper file
		switch (diversity) {
		case 1:
			fileName += "the-file-set_domain_1.txt";
			break;
		case 2:
			fileName += "the-file-set_domain_2.txt";
			break;
		case 3:
			fileName += "the-file-set_domain_3.txt";
			break;
		case 4:
			fileName += "the-file-set_domain_4.txt";
			break;
		case 5:
			fileName += "the-file-set_domain_5.txt";
			break;
		default:
			fileName += "";
		}

		// read terms from file
		HashSet<String> terms = new HashSet<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] lineContent = line.split("\t");
				terms.add(lineContent[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
		ArrayList<String> termsAsList = new ArrayList<String>();
		termsAsList.addAll(terms);

		// take a random sample of size
		Random rd = new Random();
		Collections.shuffle(termsAsList, rd);

		String termsAsString = "";
		for (int i = 0; i < size; i++) {
			termsAsString += termsAsList.get(i) + ",";
		}

		termsAsString = termsAsString.substring(0, termsAsString.length() - 1);

		return termsAsString;
	}

	// private void writeDataWithFixedDiversity(int diversity,
	// TreeMap<Integer,TreeMap<Integer,Result>> data, String fileName) {
	// BufferedWriter out;
	// try {
	// out = new BufferedWriter(new FileWriter(fileName));
	// String line = "";
	// for(Integer size : data.keySet()) {
	// // TODO: consider STDV
	// line += size + " " + data.get(size).get(diversity) + " " + "0.0";
	// out.write(line);
	// out.newLine();
	// }
	// out.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// private void writeDataWithFixedSize(int size,
	// TreeMap<Integer,TreeMap<Integer,Result>> data, String fileName) {
	// BufferedWriter out;
	// try {
	// out = new BufferedWriter(new FileWriter(fileName));
	// String line = "";
	// TreeMap<Integer,Result> res = data.get(size);
	// for(Integer div : res.keySet()) {
	// // TODO: consider STDV
	// line += div + " " + res.get(div) + " " + "0.0";
	// out.write(line);
	// out.newLine();
	// }
	// out.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	/**
	 * Retrieves the results for the given input terms from BiOSS and returns
	 * them as a list of ontology sets.
	 * 
	 * @param inputTerms
	 * @return
	 */
	private List<OntologySet> getBiOSSResults(String inputTerms, BiOSSParameters biossParams) {
		//
		// Multiset<String> concepts = HashMultiset.create();
		// String ourconcepts = "atid6833, atid15455,
		// http://purl.obolibrary.org/obo/WSIO_compression_004, atid254780,
		// atid212432, atid4050,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#D002641,
		// atid29911, atid124130, atid7009, atid219971,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#G05.360.340.037,
		// atid59295, atid1079, atid1956, atid18608, atid158,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#Z01.107.567.875.510.350.200,
		// atid343339, atid20904, atid200172,
		// http://doe-generated-ontology.com/OntoAD#C0080103,
		// http://doe-generated-ontology.com/OntoAD#C0027361, atid112,
		// http://purl.obolibrary.org/obo/ENVO_00005803, atid261, atid59438,
		// http://purl.obolibrary.org/obo/TGMA_0001246, atid7075, atid235407,
		// http://purl.obolibrary.org/obo/NCBIGene_40633, atid192148,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#Z01.433.305,
		// atid996 x 2, atid229581, atid205028, atid29665, atid192562, atid7061,
		// atid528, atid292432, atid192040,
		// http://doe-generated-ontology.com/OntoAD#C1524026 x 2,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#D059646,
		// http://purl.obolibrary.org/obo/NCBIGene_11067, atid6367, atid183535,
		// http://purl.obolibrary.org/obo/ENVO_0010080, atid190349, atid5537,
		// http://phenomebrowser.net/ontologies/mesh/mesh.owl#Z01.107.567.875.350.350.200,
		// http://who.int/icf#e2253, atid41238,
		// http://purl.obolibrary.org/obo/ENVO_01000703, atid256088,
		// http://doe-generated-ontology.com/OntoAD#C0232117,
		// http://purl.obolibrary.org/obo/NCBIGene_32506, atid14195, atid13960,
		// atid13132, atid12153, atid923,
		// http://doe-generated-ontology.com/OntoAD#C0699900,
		// http://who.int/icf#d4104, atid197151, atid2128,
		// http://purl.obolibrary.org/obo/CHMO_0000152, atid20071, atid41121,
		// http://purl.obolibrary.org/obo/UBERON_0004529,
		// http://purl.obolibrary.org/obo/TGMA_0000677,
		// http://purl.obolibrary.org/obo/TGMA_0000678";
		// concepts.addAll(Arrays.asList(ourconcepts.split(", ")));

		// find concepts for the input terms
		Multiset<String> concepts = taggingService.findConcepts(inputTerms);
		log.info("ConceptTaggingService returned the following concepts: {}", concepts);

		// get results from BiOSS
		ArrayList<RecommendationResult> result;
		try {
			result = BiOSSConnector.getBiOSSRecommendations(inputTerms, biossParams);
		} catch (NoResultFromNCBORecommenderException e) {
			log.error("Couldn't get a result from NCBO Recommender for the current configuration.", e);
			return Collections.emptyList();
		}

		// package these into sets of IOntologySets and score them
		List<OntologySet> transformedResults = new ArrayList<>();

		for (RecommendationResult r : result) {

			List<OntologyInformation> ontologies = r.ontologies;
			OntologySet s = new OntologySet();
			for (OntologyInformation ontInf : ontologies) {
				String acronym = ontInf.acronym;
				log.info("Got recommendation {} from BiOSS.", acronym);
				List<Ontology> ontologiesByIds = dbService.getOntologiesByIds(acronym);
				if (ontologiesByIds.isEmpty())
					throw new IllegalArgumentException("BiOSS returned ontology " + acronym + " as a suggested ontology but this ontology is not available in the database.");
				Ontology o = ontologiesByIds.get(0).copy();
				// add the set to the result list
				s.addOntology(o);

				// score the set
				variableScoringChain.score(o, concepts);
				// constantScoringChain.score(o);
			}

			variableScoringChain.score(s, concepts);
			constantScoringChain.score(s);

			// add to transformed results
			transformedResults.add(s);

		}

		return transformedResults;
	}

	/**
	 * Retrieves the results for the given input terms from ADOS and returns
	 * them as a list of ontology sets.
	 * 
	 * @param inputTerms
	 * @param preferences
	 * @return
	 */
	public List<OntologySet> getADOSResults(String inputTerms, SelectionParameters params) {
		// Registry registry = RegistryBuilder
		// .buildAndStartupRegistry(OSProcessesModule.class);
		// IOntologyModuleSelectionService selectionService = registry
		// .getService(IOntologyModuleSelectionService.class);
		// List<OntologySet> recommendations = selectionService.selectForText(
		// inputTerms, preferences);
		// registry.shutdown();

		List<OntologySet> recommendations = selectionService.selectForText(inputTerms, params);

		return recommendations;

	}

	// taken from
	// http://www.programcreek.com/java-api-examples/java.util.zip.GZIPInputStream
	public static boolean isGZipCompressed(byte[] bytes) throws IOException {
		if ((bytes == null) || (bytes.length < 2)) {
			return false;
		} else {
			return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
					&& (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
		}
	}

	public static byte[] getGUnzippedBytes(byte[] geZippedBytes) {

		byte[] gunzippedBytes = null;

		try {

			ByteArrayInputStream gzippedowl = new ByteArrayInputStream(geZippedBytes);
			GZIPInputStream gzipInput;

			gzipInput = new GZIPInputStream(gzippedowl);

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int len = 0;

			// Extract compressed content.
			while ((len = gzipInput.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			gunzippedBytes = out.toByteArray();

			gzippedowl.close();
			gzipInput.close();
			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return gunzippedBytes;
	}

}
