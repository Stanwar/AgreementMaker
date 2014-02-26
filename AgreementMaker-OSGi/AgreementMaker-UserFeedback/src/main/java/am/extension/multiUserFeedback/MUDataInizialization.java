package am.extension.multiUserFeedback;

import java.util.ArrayList;
import java.util.List;

import am.app.Core;
import am.app.mappingEngine.AbstractMatcher;
import am.app.mappingEngine.Mapping;
import am.app.mappingEngine.AbstractMatcher.alignType;
import am.app.mappingEngine.similarityMatrix.SimilarityMatrix;
import am.app.mappingEngine.similarityMatrix.SparseMatrix;
import am.app.ontology.Node;
import am.extension.multiUserFeedback.MUExperiment;
import am.extension.userfeedback.FeedbackLoopInizialization;

public class MUDataInizialization  extends FeedbackLoopInizialization<MUExperiment> {
	List<AbstractMatcher> inputMatchers = new ArrayList<AbstractMatcher>();
	public MUDataInizialization()
	{
		super();
	}
	
	@Override
	public void inizialize(MUExperiment exp) {
		// TODO Auto-generated method stub
		SimilarityMatrix smClass=exp.initialMatcher.getFinalMatcher().getClassesMatrix().clone();
		SimilarityMatrix smProperty=exp.initialMatcher.getFinalMatcher().getPropertiesMatrix().clone();
		for(int i=0;i<smClass.getRows();i++)
			for(int j=0;j<smClass.getColumns();j++)
				smClass.setSimilarity(i, j, 0.5);
		for(int i=0;i<smProperty.getRows();i++)
			for(int j=0;j<smProperty.getColumns();j++)
				smProperty.setSimilarity(i, j, 0.5);
		smClass=prepareSMforNB(smClass);
		smProperty=prepareSMforNB(smProperty);
		
		exp.setUflClassMatrix(smClass);
		exp.setUflPropertyMatrix(smProperty);
		
		exp.classesSparseMatrix = 
				new SparseMatrix(
						Core.getInstance().getSourceOntology(),
						Core.getInstance().getTargetOntology(), 
						alignType.aligningClasses);
		
		exp.propertiesSparseMatrix = 
				new SparseMatrix(
						Core.getInstance().getSourceOntology(),
						Core.getInstance().getTargetOntology(), 
						alignType.aligningProperties);
		
		
		exp.agreegatedClassFeedback=new SparseMatrix(
				Core.getInstance().getSourceOntology(),
				Core.getInstance().getTargetOntology(), 
				alignType.aligningClasses);
		exp.agreegatedPropertiesFeedback=new SparseMatrix(
				Core.getInstance().getSourceOntology(),
				Core.getInstance().getTargetOntology(), 
				alignType.aligningProperties);
		done();
	}

	private SimilarityMatrix prepareSMforNB(SimilarityMatrix sm)
	{
		Mapping mp;
		Object[] ssv;
		for(int i=0;i<sm.getRows();i++)
			for(int j=0;j<sm.getColumns();j++)
			{
				mp = sm.get(i, j);
				ssv=getSignatureVector(mp);
				if (!validSsv(ssv))
				{ 
					sm.setSimilarity(i, j, 0.0);
				}
			}
		
		return sm;
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
}
