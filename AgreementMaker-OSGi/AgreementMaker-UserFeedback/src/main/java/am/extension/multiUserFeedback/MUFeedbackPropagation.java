package am.extension.multiUserFeedback;

import java.util.ArrayList;
import java.util.List;

import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.AbstractMatcher.alignType;
import am.app.mappingEngine.Alignment;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.similarityMatrix.SimilarityMatrix;
import am.app.mappingEngine.similarityMatrix.SparseMatrix;
import am.app.ontology.Node;
import am.extension.userfeedback.FeedbackPropagation;
import am.matcher.Combination.CombinationMatcher;

public class MUFeedbackPropagation  extends FeedbackPropagation<MUExperiment> {
		
		final double treshold_up=0.6;
		final double treshold_down=0.01;
		final double penalize_ratio=0.9;
		private MUExperiment experiment;
		List<AbstractMatcher> inputMatchers = new ArrayList<AbstractMatcher>();
		

		private Object[] addToSV(Mapping mp, Boolean label)
		{
			//initialMatcher.
			int size=inputMatchers.size();
			Node sourceNode=mp.getEntity1();
			Node targetNode=mp.getEntity2();
			AbstractMatcher a;
			Object obj=new Object();
			Object[] ssv=new Object[size+1];
			for (int i=0;i<size;i++)
			{
				a = inputMatchers.get(i);
				obj=a.getAlignment().getSimilarity(sourceNode, targetNode);
				if (obj!=null)
					ssv[i]=obj;
				else
					ssv[i]=0.0;
				
			}
			if (label)
				ssv[size]=1.0;
			else
				ssv[size]=0.0;
			return ssv;
			
		}
		
		private Object[] getSignatureVector(Mapping mp)
		{
			int size=inputMatchers.size();
			Node sourceNode=mp.getEntity1();
			Node targetNode=mp.getEntity2();
			AbstractMatcher a;
			Object[] ssv=new Object[size];
			for (int i=0;i<size;i++)
			{
				a = inputMatchers.get(i);
				ssv[i]=a.getAlignment().getSimilarity(sourceNode, targetNode);
				
			}
			return ssv;
		}
		
		private void cloneTrainingSet(Object[][] trainingSet, Object[][] vector)
		{
			
			for(int i=0;i<vector.length;i++)
				for(int j=0;j<vector[0].length;j++)
				{
					trainingSet[i][j]=vector[i][j];
				}
		}
		
		//check if the signature vector is valid. A valid signature vector must have at least one non zero element.
		private boolean validSsv(Object[] ssv)
		{
			Object obj=0.0;
			for(int i=0;i<ssv.length;i++)
			{
				if (!ssv[i].equals(obj))
					return true;
			}
			return false;
		}
		
		
		private void agregateFeedback(Mapping candidateMapping, String userFeedback)
		{
			int row=candidateMapping.getSourceKey();
			int col=candidateMapping.getTargetKey();
			double sim=0.0;
			if(candidateMapping.getAlignmentType()==alignType.aligningClasses)
			{
				sim=experiment.agreegatedClassFeedback.getSimilarity(row, col);
				if (userFeedback.equals("CORRECT"))
					sim++;
				if (userFeedback.equals("UNCORRECT"))
					sim--;
				experiment.agreegatedClassFeedback.setSimilarity(row, col, sim);
			}
			else if(candidateMapping.getAlignmentType()==alignType.aligningProperties)
			{
				sim=experiment.agreegatedPropertiesFeedback.getSimilarity(row, col);
				if (userFeedback.equals("CORRECT"))
					sim++;
				if (userFeedback.equals("UNCORRECT"))
					sim--;
				experiment.agreegatedPropertiesFeedback.setSimilarity(row, col, sim);
			}
		}
		
		private Object[][] generateTrainingSet(int numOfMatchers)
		{
			int dimClass=experiment.agreegatedClassFeedback.countNonNullCells();
			int dimProp=experiment.agreegatedPropertiesFeedback.countNonNullCells();
			int count=0;
			Mapping m=null;
			Object[][] obj=new Object[dimClass+dimProp][numOfMatchers+1];
			if (dimClass>0)
				for(int i=0;i<experiment.agreegatedClassFeedback.getRows();i++)
				{
					for(int j=0; j<experiment.agreegatedClassFeedback.getColumns();j++)
					{
						m=experiment.agreegatedClassFeedback.get(i, j);
						if (m==null) continue;
						if (m.getSimilarity()>0)
							obj[count++]=addToSV(m, true);
						else
							if(m.getSimilarity()<0)
								obj[count++]=addToSV(m, false);
					}
				}
			if(dimProp>0)
				for(int i=0;i<experiment.agreegatedPropertiesFeedback.getRows();i++)
				{
					for(int j=0; j<experiment.agreegatedPropertiesFeedback.getColumns();j++)
					{
						m=experiment.agreegatedPropertiesFeedback.get(i, j);
						if (m==null) continue;
						if (m.getSimilarity()>0)
							obj[count++]=addToSV(m, true);
						else
							if(m.getSimilarity()<0)
								obj[count++]=addToSV(m, false);
					}
				}
			
			return obj;
		}
		
		
		
		
		@Override
		public void propagate( MUExperiment exp ) 
		{
			this.experiment=exp;
			
			inputMatchers=experiment.initialMatcher.getComponentMatchers();
			Mapping candidateMapping = experiment.selectedMapping;
			List<AbstractMatcher> availableMatchers = experiment.initialMatcher.getComponentMatchers();
			Object[][] trainingSet=new Object[1][availableMatchers.size()];
			
			agregateFeedback(candidateMapping, experiment.feedback);
			
			trainingSet=generateTrainingSet(availableMatchers.size());
			
			String userFeedback = experiment.feedback;
			
			trainingSet=optimizeTrainingSet(trainingSet);


			SimilarityMatrix feedbackClassMatrix=experiment.getUflClassMatrix();
			SimilarityMatrix feedbackPropertyMatrix=experiment.getUflPropertyMatrix();
			Mapping m=null;
			if( candidateMapping.getAlignmentType() == alignType.aligningClasses ) 
			{
				m = feedbackClassMatrix.get(candidateMapping.getSourceKey(), candidateMapping.getTargetKey());
				if( m == null ) 
					m = new Mapping(candidateMapping);
				
				if( userFeedback.equals("CORRECT") ) 
				{ 

					feedbackClassMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1.0);
					experiment.classesSparseMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1);
					
				}
				else if( userFeedback.equals("INCORRECT") ) 
				{ 
					feedbackClassMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 0.0);
					experiment.classesSparseMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1);
				}
				
			} 
			else if( candidateMapping.getAlignmentType() == alignType.aligningProperties ) 
			{
				m = feedbackPropertyMatrix.get(candidateMapping.getSourceKey(), candidateMapping.getTargetKey());
				if( m == null ) 
					m = new Mapping(candidateMapping);
				
				if( userFeedback.equals("CORRECT") ) 
				{ 
					feedbackPropertyMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1.0);
					experiment.propertiesSparseMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1);
				}
				else if( userFeedback.equals("INCORRECT") ) 
				{
					feedbackPropertyMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 0.0);
					experiment.propertiesSparseMatrix.setSimilarity(m.getSourceKey(), m.getTargetKey(), 1);
				}
			}
			
			
			if( candidateMapping.getAlignmentType() == alignType.aligningClasses )
			{
				feedbackClassMatrix=com(experiment.classesSparseMatrix , feedbackClassMatrix, trainingSet, "classes");
			}
			else
			{
				if( candidateMapping.getAlignmentType() == alignType.aligningProperties ) 
				{
					feedbackPropertyMatrix=com(experiment.propertiesSparseMatrix, feedbackPropertyMatrix, trainingSet, "properties");
				}
			}
			
			AbstractMatcher ufl=new CombinationMatcher();
			ufl.setClassesMatrix(feedbackClassMatrix);
			ufl.setPropertiesMatrix(feedbackPropertyMatrix);

			experiment.setMLAlignment(combineResults(ufl));
		
			experiment.setUflClassMatrix(feedbackClassMatrix);
			experiment.setUflPropertyMatrix(feedbackPropertyMatrix);
			
			done();
		}
		
		
		private Object[][] optimizeTrainingSet(Object[][] set)
		{
			int count=0;
			List<Integer> pos=new ArrayList<Integer>();
			for(int i=0;i<set.length;i++)
			{
				count=0;
				for(int j=0;j<set[0].length;j++)
				{
					if((Double)set[i][j]==0.0)
						count++;
				}
				if (count==set[0].length)
					pos.add(i);
			}
			Object[][] newSet=new Object[set.length-pos.size()][set[0].length];
			int index=0;
			if (pos.size()!=0)
			{
				for(int i=0;i<set.length;i++)
				{
					if (pos.contains(i))
						continue;
					
					for(int j=0;j<set[0].length;j++)
					{
						newSet[index][j]=set[i][j];
					}
					index++;
				}
				set=newSet;
			}
			
			return set;
		}
		
				
		private Alignment<Mapping> combineResults(AbstractMatcher am)
		{
			Alignment<Mapping> alg=new Alignment<Mapping>(0,0);
			int row=am.getClassesMatrix().getRows();
			int col=am.getClassesMatrix().getColumns();
			double ufl_sim=0;
			for (int i=0;i<row;i++)
			{
				for(int j=0;j<col;j++)
				{
					ufl_sim=am.getClassesMatrix().getSimilarity(i, j);
					if (ufl_sim!=0.0)
						alg.add(experiment.initialMatcher.getFinalMatcher().getClassesMatrix().get(i, j));
				}
			}
			row=am.getPropertiesMatrix().getRows();
			col=am.getPropertiesMatrix().getColumns();
			ufl_sim=0;
			for (int i=0;i<row;i++)
			{
				for(int j=0;j<col;j++)
				{
					ufl_sim=am.getPropertiesMatrix().getSimilarity(i, j);
					if (ufl_sim!=0.0)
						alg.add(experiment.initialMatcher.getFinalMatcher().getPropertiesMatrix().get(i, j));
				}
			}
			
			return alg;
		}
		
		

		private SimilarityMatrix com(SparseMatrix forbidden_pos, SimilarityMatrix sm,Object[][] trainingSet, String type)
		{
			Mapping mp;
			Object[] ssv;
			
			double distance=0;
			double min=Double.MAX_VALUE;
			int index=0;
			for(int k=0;k<sm.getRows();k++)
			{
				for(int h=0;h<sm.getColumns();h++)
				{
					if(forbidden_pos.getSimilarity(k, h)==1)
						continue;
					mp = sm.get(k, h);
					ssv=getSignatureVector(mp);
					if (!validSsv(ssv))
						continue;
					min=Double.MAX_VALUE;
					for(int i=0;i<trainingSet.length;i++)
					{
						distance=0;
						for(int j=0;j<ssv.length;j++)
						{
							distance+=Math.pow((double)ssv[j]-(double)trainingSet[i][j],2);
						}
						distance=Math.sqrt(distance);
						if (distance<min)
						{
							min=distance;
							index=i;
						}
					}
					if (min==0)
					{
						sm.setSimilarity(k, h, (double)trainingSet[index][trainingSet[0].length-1]);
						if (type=="classes")
							experiment.classesSparseMatrix.setSimilarity(k, h, 1);
						else
							experiment.propertiesSparseMatrix.setSimilarity(k, h, 1);
						
					}
				}
			}
			return sm;
		}
		

	}
