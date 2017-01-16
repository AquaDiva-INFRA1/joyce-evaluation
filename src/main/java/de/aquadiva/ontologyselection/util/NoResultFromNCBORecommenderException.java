package de.aquadiva.ontologyselection.util;

public class NoResultFromNCBORecommenderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3783147155259761594L;

	public NoResultFromNCBORecommenderException() {
	}

	public NoResultFromNCBORecommenderException(String message) {
		super(message);
	}

	public NoResultFromNCBORecommenderException(Throwable cause) {
		super(cause);
	}

	public NoResultFromNCBORecommenderException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoResultFromNCBORecommenderException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
