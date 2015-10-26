package am.matcher.myMatcher;




import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.HashBag;
import org.openrdf.sail.rdbms.schema.HashTable;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;

import am.Utility;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.Alignment;
import am.app.mappingEngine.DefaultMatcherParameters;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.ReferenceEvaluationData;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentMatcher;
import am.app.mappingEngine.referenceAlignment.ReferenceAlignmentParameters;
import am.app.mappingEngine.referenceAlignment.ReferenceEvaluator;
import am.app.mappingEngine.similarityMatrix.SimilarityMatrix;
import am.app.ontology.Node;
import am.app.ontology.Ontology;
import am.app.ontology.ontologyParser.OntoTreeBuilder;
import am.app.ontology.ontologyParser.OntologyDefinition;
import am.app.ontology.profiling.manual.ManualOntologyProfiler;
import edu.smu.tspell.wordnet.*;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import rita.*;




public class MyMatcher  extends AbstractMatcher  {

	private static String ONTOLOGY_BASE_PATH ="src/main/java/am/matcher/myMatcher/conference_dataset/"; // Use your base path
	private static String[] confs = {"cmt","conference","confOf","edas","ekaw","iasted","sigkdd"};
	private static int count;
	private static int counter;
	private static int xx_count;


	public MyMatcher(){
		super();
		count = 0;
		setName("MyMatcher");
		setCategory(MatcherCategory.UNCATEGORIZED);		
	}
	/*
	 *  Calculating similarity through cosine similarity 
	 * */
	public double getCosineSimilarity(Double[] sourceVector , Double[] targetVector){
		
		double product = 0.0;
		double normalSource = 0.0;
		double normalTarget = 0.0;
		for (int i = 0; i < sourceVector.length; i++) {
			product +=  ((sourceVector[i]) * (targetVector[i]));
			normalSource += Math.pow(sourceVector[i], 2);
			normalTarget += Math.pow(targetVector[i], 2);
			
		 }
		
		return product/(Math.sqrt(normalSource)*Math.sqrt(normalTarget));
		
	}
	
	
	/*
	 * Processing string to clean  
	 * */
	protected String curateString(String s){
		s = s.replace("-", "_");
		s = s.replace("_","");
		return s;
	}
	
	/*
	 * Checking out ancestors of both source and target
	 * */
	
	protected double ancestorScore(Node Source, Node Target){
		double cosSim = 0.0d;
		LinkedHashMap<String, Integer> hSource = new LinkedHashMap<>();
		LinkedHashMap<String, Integer> hTarget = new LinkedHashMap<>();

		hSource.put(curateString(Source.getLocalName().toLowerCase()), 0);
		hTarget.put(curateString(Target.getLocalName().toLowerCase()), 0);
		
		int count =1;
		for(Node s : Source.getAncestors()){
			
			hSource.put(curateString(s.getLocalName().toLowerCase()), count);
			count++;
		}
		
		count = 1;
		
		for(Node s : Target.getAncestors()){
			
			hTarget.put(curateString(s.getLocalName().toLowerCase()), count);
			count++;
		}
		
		
		for (HashMap.Entry<String, Integer> entry : hSource.entrySet()) {
			for(HashMap.Entry<String, Integer> targetEntry : hTarget.entrySet()){
				
				if(targetEntry.getKey().equalsIgnoreCase(entry.getKey())){
					if(targetEntry.getValue() <= entry.getValue()){
						if (entry.getValue() == 0){
							cosSim = 0.4d;
						}
						else if( entry.getValue() == 1){
							cosSim = 0.35d;
						}
						else if(entry.getValue() == 2){
							cosSim = 0.30d;
						}
						else{
							cosSim = 0.0d;
						}
					}
					else{
						if(targetEntry.getValue() == 0 ){
							cosSim = 0.4d;
						}
						else if(targetEntry.getValue() == 1 ){
							cosSim = 0.35d;
						}
						else if(targetEntry.getValue() == 2 ){
							cosSim = 0.30d;
						}
						else{
							cosSim = 0.0d;
						}
					}
					
				}
			}
		    //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
		return cosSim;

	}
	/*
	 * Remove Stop Words from source or target. This is for improving accuracy 
	 * */
	protected boolean removeStopWords(String word){
		
		if(word.equalsIgnoreCase("the") || 
				   word.equalsIgnoreCase("is") || 
				   word.equalsIgnoreCase("this") || 
				   word.equalsIgnoreCase("are") || 
				   word.equalsIgnoreCase("to") || 
				   word.equalsIgnoreCase("a") ||
				   word.equalsIgnoreCase("e") ||
				   word.equalsIgnoreCase("an") || 
				   word.equalsIgnoreCase("in") ||
				   word.equalsIgnoreCase("or") ||
				   word.equalsIgnoreCase("and") || 
				   word.equalsIgnoreCase("for") || 
				   word.equalsIgnoreCase("that") ||
				   word.equalsIgnoreCase("of") || 
				   word.equalsIgnoreCase("has")) {
			return true;
		}
		return false;
	}
	/*
	 * Function to check dataRange 
	 * */
	protected boolean checkDataRange(String word){
		
		if(word.equalsIgnoreCase("anyURI") || 
				   word.equalsIgnoreCase("base64Binary") || 
				   word.equalsIgnoreCase("boolean") || 
				   word.equalsIgnoreCase("byte") || 
				   word.equalsIgnoreCase("dateTime") || 
				   word.equalsIgnoreCase("dateTimeStamp") ||
				   word.equalsIgnoreCase("decimal") ||
				   word.equalsIgnoreCase("double") || 
				   word.equalsIgnoreCase("float") ||
				   word.equalsIgnoreCase("hexBinary") ||
				   word.equalsIgnoreCase("int") || 
				   word.equalsIgnoreCase("integer") || 
				   word.equalsIgnoreCase("Literal") || 
				   word.equalsIgnoreCase("short") ||
				   word.equalsIgnoreCase("Literal") ||
				   word.equalsIgnoreCase("long") || 
				   word.equalsIgnoreCase("for")) {
			return true;
		}
		return false;
	}
	public boolean isAbbr(String one, String two){
		// Checking out whether the source or target has abbreviations
		//
		String[] split1 = one.split("_");
		String[] split2 = two.split("_");
		char[] arr1 = split1[0].toCharArray();
		char[] arr2 = split2[0].toCharArray();
		boolean flag1 = true, flag2 = true;
		
		try{
			for(int i =0;i<arr1.length;i++){
				if(!Character.isUpperCase(arr1[i])){
					flag1 = false;
					break;
				}
			}
			for(int i =0;i<arr2.length;i++){
				if(!Character.isUpperCase(arr2[i])){
					flag2 = false;
					break;
				}
			}
			if(flag1){
				for (int i = 0;i<arr1.length;i++){
					if (Character.toLowerCase(arr1[i]) != Character.toLowerCase(split2[i].charAt(0)))
						return false;
				}
				return true;
			}
			else if(flag2){
				for (int i = 0;i<arr2.length;i++){
					if (Character.toLowerCase(arr2[i]) != Character.toLowerCase(split1[i].charAt(0)))
						return false;
				}
				return true;
			}
			else{
				return false;
			}
		}
		catch(Exception Ex){
			return false;
		}
		
	}
	@Override
	protected Mapping alignTwoNodes(Node source, Node target,
			alignType typeOfNodes, SimilarityMatrix matrix) throws Exception {

			
			NounSynset nounSynset;
			NounSynset[] hyponyms;
			// Wordnet File Path Please change when running the program.
			System.setProperty("wordnet.database.dir","/usr/local/WordNet-3.0/dict");
			
			WordNetDatabase database = WordNetDatabase.getFileInstance();
			double cosSim = 0.0d;
			double nounSim = 0.0d;
			double verbSim = 0.0d;
			//testWriteFile(matrix);
			
			String sourceLocalName;
			String targetLocalName;
			
			
			sourceLocalName = curateString(source.getLocalName());
			targetLocalName = curateString(target.getLocalName());
			
			HashMap<String, Integer> sourceSet = new HashMap();
			HashMap<String, Integer> targetSet = new HashMap();
 /*
  *  Algorithm : 
  *  We are trying to find similarity between source and target nodes 
  *  1. Split into classes and property 
  *  2. If exact match, break and sim score is 1.0
  *  3. If not, check length and if same, check if the characters are same. 
  *  4. Preprocess using stop word removal and splitting based on whitespace or camelcase.
  *  5. Calculate similarity score based on number of occurences.
  *  6. If the score is less than 0.3, apply wordnet
  *  7. For Properties, follow the same steps .  
  *  8. For Properties, check the domain and range condition 
  *  
  * */
				if( typeOfNodes.toString().equalsIgnoreCase("aligningClasses")){
					
					if(source.getLocalName().equalsIgnoreCase("email") && target.getLocalName().equalsIgnoreCase("E-mail") ){
						System.out.println("Stop");
					}
					if(sourceLocalName.equalsIgnoreCase(targetLocalName))
						cosSim=1.0d;
					/*else if(isAbbr(source.getLocalName(), target.getLocalName())){
						cosSim =1.0d;
					}*/
					else{ 
						
						String x1 = source.getLocalName().replace("-", "");
						x1 = x1.replace("_", " ");
						x1 = x1.replace(".", " ");
						String x2 = target.getLocalName().replace("-", "");
						x2 = x2.replace("_", " ");
						x2 = x2.replace(".", " ");
						
						if(x1.equalsIgnoreCase(x2)){
							cosSim = 1.0d;
						}
						if (x1.length() == x2.length()){
					    	int same = 0;
						    for (int i = 0; i < x1.length(); i++) {
						        if (x1.charAt(i) == x2.charAt(i))
						            same++;
						    }
						    if(same> x1.length() - 2){
						    	cosSim = 1.0;
						    }

						    char[] first = x1.toCharArray();
							  char[] second = x2.toCharArray();
							  Arrays.sort(first);
							  Arrays.sort(second);
							  Boolean result =  Arrays.equals(first, second);
							  
							  if(result){
								  cosSim = 1.0;
							  }
					    }
						else{
							// Finding Patterns - Trying to find whitespace and splitting based on that 
							// We have to split it because of multiple occurances for camel cases and lowercase. 
							Pattern pattern = Pattern.compile("\\s");
							Matcher matcher = pattern.matcher(x1);
							boolean found = matcher.find();
							matcher = pattern.matcher(x2);
							boolean found2 = matcher.find();

							sourceSet.clear();
							targetSet.clear();
							
							// For Whitespace
							if(found && x1.length() > 1){
								String[] s = x1.split(" ");
								for(String ss : s){
									if(!removeStopWords(ss)){
										sourceSet.put(ss.toLowerCase(),0);
										//System.out.println("word: " + ss.toLowerCase());
									}
								
								}
								
							}else{
								// For CamelCase
								String[] s = x1.split("(?=\\p{Upper})");
								for(String ss : s){
									if(!removeStopWords(ss)){
										sourceSet.put(ss.toLowerCase(),0);
										//System.out.println("word: " + ss.toLowerCase());
									}
								
								}
								
							}
								
						
							if(found2 && x2.length() > 1){
								String[] t = x2.split(" ");
								// For WhiteSpace
								for(String tt : t){
																					
									if(!removeStopWords(tt)){
										targetSet.put(tt.toLowerCase(),0);
									}
								}
							}else{
								// FOR CAMELCASE
								String[] t = x2.split("(?=\\p{Upper})");
								for(String tt : t){
									if(!removeStopWords(tt)){
										targetSet.put(tt.toLowerCase(),0);
									}
								}
								
							}
							
							int checkCounter = 0;
							
							// Starting counter and a comparison. We are trying to find occurances of matching words. Using that we will 
							// take out the similarity
							for (HashMap.Entry<String, Integer> entry : sourceSet.entrySet()) {
								for(HashMap.Entry<String, Integer> targetEntry : targetSet.entrySet()){
									if(targetEntry.getKey().equalsIgnoreCase(entry.getKey()) ){
										checkCounter++;
									}
								}
							}
							// Calculating similarity 
							cosSim = (double) (checkCounter)/(double) Math.max(sourceSet.size() ,targetSet.size()) ;
							
						}
						//
						// Run wordnet if the score is below .3. 
						//
						if(cosSim <0.3d ){
							double tempSim = 0.0d;
							double totalSim = 0.0d;
							if(sourceSet.size() == 1 && targetSet.size() == 1){
								for(String in : sourceSet.keySet()){
									// Removing stop words to boost accuracy 
									if(!removeStopWords(in)){
										for(String tar : targetSet.keySet()){
											if(!(removeStopWords(tar))){
												// Split words based on whether Noun or Verb 
												Synset[] synsets = database.getSynsets(in, SynsetType.NOUN );
												Synset[] synsets2 = database.getSynsets(tar, SynsetType.NOUN);
												
												Synset[] synsetsVerb = database.getSynsets(in, SynsetType.VERB );
												Synset[] synsetsVerb2 = database.getSynsets(tar, SynsetType.VERB);
												Hashtable sourceTable = new Hashtable();
												
												// Create three bag for source, target and total 
												if(synsets.length != 0 && synsets2.length != 0 ){
													HashBag sourceBag = new HashBag();
													HashBag targetBag = new HashBag();
													HashBag BagOfWords = new HashBag();
													// For Source Nouns
													for( int i = 0; i < synsets.length; i++ ) {
														String[] words = synsets[i].getWordForms();
									
														for( int j = 0; j < words.length; j++ ) {
															int number = synsets[i].getTagCount(words[j]);
															
																if(number > 0 && !sourceLocalName.equalsIgnoreCase(words[j])){
																	words[j].replace("_", "");
																	words[j].replace(" ", "");
																	sourceBag.add(words[j]);
																	BagOfWords.add(words[j]);
																}
														}
													}
													// For Target Nouns	
													for( int i = 0; i < synsets2.length; i++ ) {
														String[] words = synsets2[i].getWordForms();
														
														for( int j = 0; j < words.length; j++ ) {
															int number = synsets2[i].getTagCount(words[j]);
															
																if(number > 0 && !sourceLocalName.equalsIgnoreCase(words[j])){
																	words[j].replace("_", "");
																	words[j].replace(" ", "");
																	//System.out.print(words[j]);
																	targetBag.add(words[j]);
																	BagOfWords.add(words[j]);
																}
															
														}
														
													}
													// Creating Vectors for source and target	
													Double[] sourceVector = new Double[BagOfWords.uniqueSet().size()];
													Double[] targetVector = new Double[BagOfWords.uniqueSet().size()];
													
													
													Object[] arl =  BagOfWords.uniqueSet().toArray();
													
													for(int i=0;i<BagOfWords.uniqueSet().size();i++){
														if( sourceBag.contains(arl[i])){
															sourceVector[i] = 1.0; 
														}
														else{
															sourceVector[i] = 0.0 ;
														}
														if(targetBag.contains(arl[i])){
															targetVector[i] = 1.0;
														}
														else{
															targetVector[i] = 0.0;
														}
													}
													// Calculating Noun Similarity by using Cosine Similarity
													nounSim = getCosineSimilarity(sourceVector, targetVector);
														
												}
												// Check for Verbs
												else 
												if(synsetsVerb.length != 0 && synsetsVerb2.length !=0) {
													HashBag sourceBag = new HashBag();
													HashBag targetBag = new HashBag();
													HashBag BagOfWords = new HashBag();
													for( int i = 0; i < synsetsVerb.length; i++ ) {
														String[] words = synsetsVerb[i].getWordForms();
														
														for( int j = 0; j < words.length; j++ ) {
															int number = synsetsVerb[i].getTagCount(words[j]);
																// Creating bags
																if(number > 0 && !sourceLocalName.equalsIgnoreCase(words[j])){		
																	words[j].replace("_", "");
																	words[j].replace(" ", "");
																	//System.out.print(words[j]);
																	sourceBag.add(words[j]);
																	BagOfWords.add(words[j]);
																}
															}
														}
													
													for( int i = 0; i < synsetsVerb2.length; i++ ) {
														String[] words = synsetsVerb2[i].getWordForms();
														
														for( int j = 0; j < words.length; j++ ) {
															int number = synsetsVerb2[i].getTagCount(words[j]);
															
																if(number > 0 && !sourceLocalName.equalsIgnoreCase(words[j])){
																	words[j].replace("_", "");
																	words[j].replace(" ", "");
																	targetBag.add(words[j]);
																	BagOfWords.add(words[j]);
																}
															//}
														}
														//System.out.println(".");
													}
													// Vectors for Verbs both source and target	
													Double[] sourceVector = new Double[BagOfWords.uniqueSet().size()];
													Double[] targetVector = new Double[BagOfWords.uniqueSet().size()];
													
													
													Object[] arl =  BagOfWords.uniqueSet().toArray();
													
													for(int i=0;i<BagOfWords.uniqueSet().size();i++){
														// 1.0 if bag contains word 0.0 if doesnt contain word
														if( sourceBag.contains(arl[i])){
															sourceVector[i] = 1.0; 
														}
														else{
															sourceVector[i] = 0.0 ;
														}
														if(targetBag.contains(arl[i])){
															targetVector[i] = 1.0;
														}
														else{
															targetVector[i] = 0.0;
														}
														//System.out.println(sourceVector[i]);
														}
														verbSim = getCosineSimilarity(sourceVector, targetVector);
													}
									
													// Take the maximum similarity score- either from noun and verb
													if( nounSim >= verbSim ){
														tempSim = nounSim;
													}
													else {
														tempSim = verbSim;
													}
													
													totalSim = totalSim + tempSim;
//													// Replace with overall similarity score if greater.
//													if(tempSim >= cosSim){
//														cosSim = tempSim;
//													}
												}
											}
												
										}
								}
								totalSim = totalSim/Math.max(sourceSet.size(), targetSet.size());
								
								if(totalSim > cosSim && totalSim > .09){
									cosSim = totalSim;
								}
							}			
						}
					}
				}else 
					if (typeOfNodes.toString().equalsIgnoreCase("aligningProperties")){
						/*
						 * Finding similarity score for Properties
						 * */
						
						// Preprocessing by removing hyphens and replacing underscore with space
						String x1 = source.getLocalName().replace("-", "");
						
						x1 = x1.replace("_", " ");
						x1 = x1.replace(".", " ");
						String x2 = target.getLocalName().replace("-", "");
						x2 = x2.replace("_", " ");
						x2 = x2.replace(".", " ");
						
						String sName = x1.replace("_", " ");
						String tName = x2.replace("_", " ");
						
						// If the source name and target name is same
						try{
							if(x1.equalsIgnoreCase(x2)){
								// Check domain and range if exist, then match. We are taking only those properties which have the same domain and range
								if((source.getPropertyDomain().getLocalName() != null) && (target.getPropertyDomain().getLocalName() !=null) 
										&& (source.getPropertyRange().getLocalName() != null) && (target.getPropertyRange().getLocalName() != null)){
									if(source.getPropertyDomain().getLocalName().equalsIgnoreCase(target.getPropertyDomain().getLocalName())  && source.getPropertyRange().getLocalName().equalsIgnoreCase(target.getPropertyRange().getLocalName())){
										cosSim = 1.0d;
									}
									
								}
								// Check if the property is a data property. If yes, then check only the range and match 
								else
									if(checkDataRange(source.getPropertyRange().getLocalName()) || checkDataRange(target.getPropertyRange().getLocalName())){
										cosSim = 1.0d;
									}
								else{
										cosSim =0.0d;
									}
							}
							else{
								try{
									// If property and range exists
									if((source.getPropertyDomain().getLocalName() != null) && (target.getPropertyDomain().getLocalName() !=null) 
											&& (source.getPropertyRange().getLocalName() != null) && (target.getPropertyRange().getLocalName() != null)){
										
										if(source.getPropertyDomain().getLocalName().equalsIgnoreCase(target.getPropertyDomain().getLocalName())  && source.getPropertyRange().getLocalName().equalsIgnoreCase(target.getPropertyRange().getLocalName())){
											// Check if the length is same, match all the characters 
											// and if they are same, then sim = 1.0 else 0 
											if (x1.length() == x2.length()){
												// Checking characters
											    	int same = 0;
												    for (int i = 0; i < x1.length(); i++) {
												    	
												        if (x1.charAt(i) == x2.charAt(i))
												            same++;
												    }
												    if(same> x1.length() - 2){
												    	cosSim = 1.0;
												    }
												   
												    char[] first = x1.toCharArray();
													  char[] second = x2.toCharArray();
													  
													  // Sorting arrays
													  Arrays.sort(first);
													  Arrays.sort(second);
													  Boolean result =  Arrays.equals(first, second);
													  
													  if(result){
														  cosSim = 1.0;
													  }
											}else{
												// Checking patterns again 
												Pattern pattern = Pattern.compile("\\s");
												Matcher matcher = pattern.matcher(x1);
												boolean found = matcher.find();
												matcher = pattern.matcher(x2);
												boolean found2 = matcher.find();
												
												sourceSet.clear();
												targetSet.clear();
												if(found && x1.length() > 1){
													String[] s = sName.split(" ");
													for(String ss : s){
														if(!removeStopWords(ss)){
															sourceSet.put(ss.toLowerCase(),0);
														}
													
													}
													
												}else{
													String[] su = sName.split("(?=\\p{Upper})");
													for(String ssu : su){
														if(!removeStopWords(ssu)){
															sourceSet.put(ssu.toLowerCase(),0);
														}
													}
												}
												
												
												if(found2 && x2.length() > 1){
													String[] t = tName.split(" ");
													for(String tt : t){
																										
														if(!removeStopWords(tt)){
															targetSet.put(tt.toLowerCase(),0);
														}
													}
												}else{
													String[] tu = tName.split("(?=\\p{Upper})");
													for(String ttu : tu){
														if(!removeStopWords(ttu)){
															targetSet.put(ttu.toLowerCase(),0);
														}
													}
												}

												int checkCounter = 0;
												for (HashMap.Entry<String, Integer> entry : sourceSet.entrySet()) {
													for(HashMap.Entry<String, Integer> targetEntry : targetSet.entrySet()){
														if(targetEntry.getKey().equalsIgnoreCase(entry.getKey()) ){
															checkCounter++;
														}
													}
												}
												cosSim = (double) (checkCounter)/(double) Math.max(sourceSet.size(), targetSet.size());
											}
											
										}
									}
								}
								catch(Exception Ex){
									cosSim = 0.0d;
								}
							
							}
						}
						catch(Exception e){
							cosSim = 0.0d;
						}
						
				}

			if(Double.isNaN(cosSim)){
				cosSim = 0.0d;
			}
			
		
			if(cosSim < 0.51 && typeOfNodes.toString().equalsIgnoreCase("aligningClasses")){
				cosSim = 0.0d;
			} 
			if(cosSim > 1.0){
				cosSim = 1.0;
			}
//			
//			if (cosSim != 0.0d){
//				counter++;
//				//System.out.println(" Source : " + source.getLocalName() + "  Target : " + target.getLocalName() + " CosSim : " + cosSim + " TypeOfNode : "  + typeOfNodes + " maxValue : " + matrix.getMaxValue());
//				 
//			}
			
		return new Mapping(source, target, cosSim);
	}


	private ArrayList<Double> referenceEvaluation(String pathToReferenceAlignment)
			throws Exception {

	
		// Run the reference alignment matcher to get the list of mappings in
		// the reference alignment
		ReferenceAlignmentMatcher refMatcher = new ReferenceAlignmentMatcher();


		// these parameters are equivalent to the ones in the graphical
		// interface
		ReferenceAlignmentParameters parameters = new ReferenceAlignmentParameters();
		parameters.fileName = pathToReferenceAlignment;
		parameters.format = ReferenceAlignmentMatcher.OAEI;
		parameters.onlyEquivalence = false;
		parameters.skipClasses = false;
		parameters.skipProperties = false;
		refMatcher.setSourceOntology(this.getSourceOntology());
		refMatcher.setTargetOntology(this.getTargetOntology());

		// When working with sub-superclass relations the cardinality is always
		// ANY to ANY
		if (!parameters.onlyEquivalence) {
			parameters.maxSourceAlign = AbstractMatcher.ANY_INT;
			parameters.maxTargetAlign = AbstractMatcher.ANY_INT;
		}

		refMatcher.setParam(parameters);

		// load the reference alignment
		refMatcher.match();
		//System.out.println(refMatcher.getAlignment());
		//System.out.println(refMatcher.getAlignmentsStrings());
		Alignment<Mapping> referenceSet;
		if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
			referenceSet = refMatcher.getAlignment(); // class + properties
		} else if (refMatcher.areClassesAligned()) {
			referenceSet = refMatcher.getClassAlignmentSet();
		} else if (refMatcher.arePropertiesAligned()) {
			referenceSet = refMatcher.getPropertyAlignmentSet();
		} else {
			// empty set? -- this should not happen
			referenceSet = new Alignment<Mapping>(Ontology.ID_NONE,
					Ontology.ID_NONE);
		}

		// the alignment which we will evaluate
		Alignment<Mapping> myAlignment;

		if (refMatcher.areClassesAligned() && refMatcher.arePropertiesAligned()) {
			myAlignment = getAlignment();
		} else if (refMatcher.areClassesAligned()) {
			myAlignment = getClassAlignmentSet();
		} else if (refMatcher.arePropertiesAligned()) {
			myAlignment = getPropertyAlignmentSet();
		} else {
			myAlignment = new Alignment<Mapping>(Ontology.ID_NONE,
					Ontology.ID_NONE); // empty
		}
		
		// use the ReferenceEvaluator to actually compute the metrics
		ReferenceEvaluationData rd = ReferenceEvaluator.compare(myAlignment,
				referenceSet);

		// optional
		setRefEvaluation(rd);

		// output the report
		StringBuilder report = new StringBuilder();
		report.append("Reference Evaluation Complete\n\n").append(getName())
				.append("\n\n").append(rd.getReport()).append("\n");
		
		
		double precision=rd.getPrecision();
		double recall=rd.getRecall();
		double fmeasure=rd.getFmeasure();
		
		
		ArrayList<Double> results=new ArrayList<Double>();
		results.add(precision);
		results.add(recall);
		results.add(fmeasure);
		
		return results;
		
		//log.info(report);
		
		// use system out if you don't see the log4j output
	  	

	}

	
	
	public static void main(String[] args) throws Exception {
	
		MyMatcher mm = new MyMatcher();
		double precision=0.0d;
		double recall=0.0d;
		double fmeasure=0.0d;
		int size=21;
		for(int i = 0; i < confs.length-1; i++)
		{
			for(int j = i+1; j < confs.length; j++)
			{
				Ontology source = OntoTreeBuilder.loadOWLOntology(ONTOLOGY_BASE_PATH + "/"+confs[i]+".owl");
				Ontology target = OntoTreeBuilder.loadOWLOntology(ONTOLOGY_BASE_PATH + "/"+confs[j]+".owl");
				
				OntologyDefinition def1=new OntologyDefinition(true, source.getURI(), null, null);
				OntologyDefinition def2=new OntologyDefinition(true, target.getURI(), null, null);
		
				def1.largeOntologyMode=false;
				source.setDefinition(def1);
				def2.largeOntologyMode=false;
				target.setDefinition(def2);
				ManualOntologyProfiler mop=new ManualOntologyProfiler(source, target);
				mm.setSourceOntology(source);
				mm.setTargetOntology(target);
		
				DefaultMatcherParameters param = new DefaultMatcherParameters();
		
		//Set your parameters
				param.threshold = 0.0;
				param.maxSourceAlign = 1;
				param.maxTargetAlign = 1;
			//	mm.setName(TARGET_ONTOLOGY);
				mm.setParameters(param);

				try {
					mm.match();			
			
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			ArrayList<Double> results=	mm.referenceEvaluation(ONTOLOGY_BASE_PATH + confs[i]+"-"+confs[j]+".rdf");
			precision+=results.get(0);
			recall+=results.get(1);
			fmeasure+=results.get(2);
			
			
			}
			
		}

		StringBuilder sb= new StringBuilder();
		
		precision/=size;
		recall/=size;
		fmeasure/=size;
		
		String pPercent = Utility.getOneDecimalPercentFromDouble(precision);
		String rPercent = Utility.getOneDecimalPercentFromDouble(recall);
		String fPercent = Utility.getOneDecimalPercentFromDouble(fmeasure);
		
		sb.append("Total Matches : " + counter);
		sb.append("Precision = Correct/Discovered: "+ pPercent+"\n");
		sb.append("Recall = Correct/Reference: "+ rPercent+"\n");
		sb.append("Fmeasure = 2(precision*recall)/(precision+recall): "+ fPercent+"\n");
	
		String report=sb.toString();
		System.out.println("Evaulation results:");
		System.out.println(report);

	}
	
 

	
}
