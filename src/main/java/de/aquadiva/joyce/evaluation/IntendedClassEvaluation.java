package de.aquadiva.joyce.evaluation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class IntendedClassEvaluation {
	private static OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

	public static void main(String[] args) throws IOException {
		
		//get all possible IRIs for the input terms and the annotation (from annotation file)
		Map<String, Intended> intendedClassesInDirectory = readIntendedClassesInDirectory(new File("/home/friedi/workspace/TermSearch/src/annotated-terms/flux-annotated/final/"));
		for (Entry<String, Intended> entry : intendedClassesInDirectory.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		
		//get all possible IRIs as HashSet and get all correct IRIs as HashSet
		HashSet<String> possibleClassIRIs = new HashSet<String>();
		HashSet<String> possibleAndCorrectClassIRIs = new HashSet<String>();
		for (String iri : intendedClassesInDirectory.keySet()) {
			possibleClassIRIs.add(iri);
			if(intendedClassesInDirectory.get(iri).equals(Intended.YES)) {
				possibleAndCorrectClassIRIs.add(iri);
			}
		}
		
		System.out.println(possibleClassIRIs.size());
		System.out.println(possibleAndCorrectClassIRIs.size());

		//get all found IRIs from results folder
		String owlDirName = "/home/friedi/workspace/TermSearch/src/annotated-terms/flux-annotated/final/setting-ados-cluster-211-30-100-30-5-flux4/result0";
		List<String> owlFileNames = ADOSEval.fileList(owlDirName);
		HashSet<String> foundClassIRIs = new HashSet<String>();
		
		for(String owlFileName : owlFileNames) {
			if( owlFileName.contains(".owl") ) {
				byte[] owl = null;
				try {
					owl = IOUtils.toByteArray(new FileInputStream(new File(owlFileName)));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					foundClassIRIs.addAll(getClassIrisInOwlOntologies( ontologyManager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(owl)) ));
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println(foundClassIRIs.size());
		
		//determine the number of the found class IRIs which belong to input terms
		foundClassIRIs.retainAll(possibleClassIRIs);
		int numOfFoundIRIs = foundClassIRIs.size();
		
		//determine the number of correctly annotated class IRIs
		foundClassIRIs.retainAll(possibleAndCorrectClassIRIs);
		int numOfCorrectClasses = foundClassIRIs.size();

		System.out.println("fraction of correctly annotated: " + (double) numOfCorrectClasses / (double) numOfFoundIRIs);
		
		
	}
	
	private static HashSet<String> getClassIrisInOwlOntologies(OWLOntology o) {
		HashSet<String> classIris = new HashSet<>();
			Set<OWLClass> classesInSignature = o.getClassesInSignature();
			for (OWLClass c : classesInSignature) {
				classIris.add(c.getIRI().toString());
			}
		return classIris;
	}

	/**
	 * A simple enumeration to define the valid values of intention. YES and NO
	 * are clear. Sometimes we use something like 'RUBISH' or we just can't
	 * decide. Those values are represented in the enumeration by OTHER.
	 * 
	 * @author faessler
	 *
	 */
	public enum Intended {
		YES, NO, OTHER
	}

	/**
	 * In the directory <tt>dir</tt>, reads all .tsv or .csv files (however, tab
	 * separated values are expected) and takes them to be annotation files of
	 * intended vs. not-intended classes for our evaluation.
	 * 
	 * @param dir
	 *            Directory to read annotation files from.
	 * @return A map assigning to each unique class IRI a value of the
	 *         {@link Intended} enumeration.
	 * @throws IOException
	 */
	public static Map<String, Intended> readIntendedClassesInDirectory(File dir) throws IOException {
		Map<String, Intended> intendedMap = new LinkedHashMap<>();
		File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("csv") || name.endsWith("tsv");
			}

		});
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			readAnnotatedClasses(file, intendedMap);
		}

		return intendedMap;
	}

	/**
	 * Reads the file at <tt>path</tt> and takes it to be a tab separated
	 * annotation file for the intended classes for our ontology selection
	 * evaluation.
	 * 
	 * @param path
	 *            The annotation file to read.
	 * @param intendedMap
	 *            The map assigning the intention-value to each class is given
	 *            from outside as to accumulate the annotation data of multiple
	 *            files into this one, single map.
	 * @throws IOException
	 */
	public static void readAnnotatedClasses(File path, Map<String, Intended> intendedMap) throws IOException {
		LineIterator lineIterator = FileUtils.lineIterator(path);
		int lineNum = 1;
		while (lineIterator.hasNext()) {
			String line = (String) lineIterator.next();
			if (line.startsWith("term"))
				// this is the header
				continue;
			String[] split = line.split("\t");
			if (split.length < 5)
				throw new IllegalArgumentException("Format error: The " + lineNum + ". line of file "
						+ path.getAbsolutePath() + " has less than 5 columns.");
			String iri = split[2];
			Intended intended;
			try {
				intended = Intended.valueOf(split[5].toUpperCase());
			} catch (Exception e) {
				intended = Intended.OTHER;
			}
			Intended alreadyLoadedIntended = intendedMap.get(iri);
			if (null != alreadyLoadedIntended && alreadyLoadedIntended != intended)
				throw new IllegalStateException("WARNING! The file " + path.getAbsolutePath() + " defines the IRI "
						+ iri + " both as intended and not intended, thus it is inconsistent.");
			intendedMap.put(iri, intended);
			++lineNum;
		}
	}

}
