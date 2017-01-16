#!/bin/bash

export CLASSPATH=`echo lib/*.jar | tr ' ' ':'`:target/classes
java de.aquadiva.ontologyselection.evaluation.BiOSSEval