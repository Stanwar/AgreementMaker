package am.va.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import am.app.Core;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.MatchingTask;
import am.app.mappingEngine.manualMatcher.UserManualMatcher;
import am.app.mappingEngine.similarityMatrix.SimilarityMatrix;
import am.app.ontology.Node;
import am.app.ontology.Ontology;

public class VAUFL {
	List<MatchingTask> matchingTask;
	MatchingTask userTask;
	HashMap<String, VAUFLPairs> UFLSelectionMap;

	public VAUFL() {
		UFLSelectionMap = new HashMap<String, VAUFLPairs>(); // init selection
																// map
		matchingTask = Core.getInstance().getMatchingTasks(); // get matching
																// tasks for
																// later use
		userTask = matchingTask.get(0);
		setBestMatchingGroup(); // find out the best matching group and assign
								// it to userResult (matchingTask[0])
		//getAbiMatchings(VAVariables.ontologyType.Source);//get arbitrary matchings 
	}

	/**
	 * Set the user manual matcher to the best matching task
	 * 
	 * @return
	 */
	private boolean setBestMatchingGroup() {
		int len = matchingTask.size();
		int res = 0, best = -1;
		for (int i = 1; i < len; i++) {
			MatchingTask m = matchingTask.get(i);
			int tmp = 0;
			if (m.selectionResult != null && m.selectionResult.classesAlignment != null)
				tmp += m.selectionResult.classesAlignment.size();
			if (m.selectionResult != null && m.selectionResult.propertiesAlignment != null)
				tmp += m.selectionResult.propertiesAlignment.size();
			// System.out.println("number of alignments=" + tmp);
			if (tmp > res) {
				best = i;
				res = tmp;
			}
		}
		if (best == -1)
			return false;
		// update the 'best' result to userResult
		updateUserTask(matchingTask.get(best));
		return true;
	}

	/**
	 * Update the user manual matcher, called by setBestMatchingGroup()
	 * 
	 * @param bestTest
	 */
	private void updateUserTask(MatchingTask bestTest) {

		// DefaultMatcherParameters mParam = new DefaultMatcherParameters();

		UserManualMatcher m = new UserManualMatcher();
		m.setSourceOntology(Core.getInstance().getSourceOntology());
		m.setTargetOntology(Core.getInstance().getTargetOntology());

		userTask = new MatchingTask(m, bestTest.matcherParameters, bestTest.selectionAlgorithm,
				bestTest.selectionParameters);

		userTask.match();
		userTask.select();
		// System.out.println("set best matcher: class=" +
		// userTask.selectionResult.classesAlignment.size());
		// System.out.println("set best matcher: properity=" +
		// userTask.selectionResult.propertiesAlignment.size());
	}

	/**
	 * Get matching pairs generated by each group
	 * 
	 * @param type
	 */
	public void getAbiMatchings(ArrayList<VAUFLPairs> lstPairs, VAVariables.ontologyType type) {
		// iterate the source ontology concepts
		int len = matchingTask.size();
		SimilarityMatrix sMatrix;

		// For each algorithm
		for (int i = 1; i < len; i++) {
			if (type == VAVariables.ontologyType.Source) {
				sMatrix = matchingTask.get(i).matcherResult.getClassesMatrix();
				List<Node> sourceNodes = sMatrix.getSourceOntology().getClassesList();

				// For each sourceNode
				for (Node source : sourceNodes) {
					// get matching info here
					Mapping map = sMatrix.getRowMaxValues(source.getIndex(), 1)[0];
					Node target = map.getEntity2();

					if (!UFLSelectionMap.containsKey(source.getLocalName())) {
						VAUFLPairs newPair = new VAUFLPairs(source);
						newPair.addToTargetList(target);
						UFLSelectionMap.put(source.getLocalName(), newPair);
					} else {
						VAUFLPairs oldPair = UFLSelectionMap.get(source.getLocalName());
						if (!oldPair.containTarget(target.getLocalName())) {
							oldPair.addToTargetList(target);
						}
					}
				}
			}
		}

		// Get the list multi-matchings by remove single value matchings
		for (String key : UFLSelectionMap.keySet()) {
			if (UFLSelectionMap.get(key).getTargetNodes().size() > 1) {
				lstPairs.add(UFLSelectionMap.get(key));//add to list
			}
		}
	}

	private void output() {
		for (String key : UFLSelectionMap.keySet()) {
			System.out.println("Source=" + key);
			for (Map.Entry<String, Node> t : UFLSelectionMap.get(key).getTargetNodes().entrySet()) {
				String tname = t.getValue().getLocalName();
				System.out.println("(" + key + ", " + tname + ")");
			}
		}
	}
}
