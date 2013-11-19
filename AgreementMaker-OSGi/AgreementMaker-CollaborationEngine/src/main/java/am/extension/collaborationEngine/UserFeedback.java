package am.extension.collaborationEngine;

import am.app.mappingEngine.utility.MatchingPair;

public class UserFeedback {

	public enum FeedbackValue {
		CORRECT,
		INCORRECT,
		SKIP;
	}
	
	public int sourceKey;
	public int targetKey;
	
	public MatchingPair pair;
	public FeedbackValue feedback;
	
}
