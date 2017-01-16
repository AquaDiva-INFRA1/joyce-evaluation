package de.aquadiva.ontologyselection.evaluation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class DisambiguationEvaluator {
	private static OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

	public static void main(String[] args) {
		
		//the input terms
		
		//TODO fill
		String owlDirName = "";
		List<String> owlFileNames = ADOSEval.fileList(owlDirName);
		HashSet<String> foundClassIRIs = new HashSet<String>();
		
		for(String owlFileName : owlFileNames) {
			byte[] owl = null;
			try {
				owl = IOUtils.toByteArray(new FileInputStream(new File(owlFileName)));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				foundClassIRIs.addAll(getClassIrisInOwlOntologies( ontologyManager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(owl)) ));
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//load correct mappings from file
		HashSet<String> correctIRIs = new HashSet<String>();
		////home/friedi/workspace/TermSearch/src/annotated-terms/flux-annotated/final
		
		int numOfFoundIRIs = foundClassIRIs.size();
		
		foundClassIRIs.retainAll(correctIRIs);
		int numOfCorrectClasses = foundClassIRIs.size();

		System.out.println((double) numOfCorrectClasses / (double) numOfFoundIRIs);
		
	}

	private static HashSet<String> getClassIrisInOwlOntologies(OWLOntology o) {
		HashSet<String> classIris = new HashSet<>();
			Set<OWLClass> classesInSignature = o.getClassesInSignature();
			for (OWLClass c : classesInSignature) {
				classIris.add(c.getIRI().toString());
			}
		return classIris;
	}
	
}
