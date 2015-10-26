# Forked by Sharad Tanwar 

Assignment Deals with extending the matching system by checking and improving the F-Score. 

Added Multiple MyMatcher Files to the system. 

Assignment 2: Agreement Maker Sudhanshu Malhotra, Sharad Tanwar

We were provided with an ontology matching problem and we were supposed to improve the provided code for a better F-Measure score.


The code given to us had the following results:

Techniques Applied were different for classes and properties :

1. We first split the source and target into two different areas : Classes and properties. The
techniques applied to them differ and this is essential to coding for this project.
Our Algorithm checks for the structural similarity and string matching using wordnet.
1. Split into different areas
2. Apply preprocessing which includes removing hyphens, spaces, etc
3. Apply the given below techniques.
• Common Techniques applied in both :
1. Stop Words Removal: Removed the words which are common words which can
cause accuracy to go down when matching. These common are noise and therefore
removed between any type of processing is down.
2. String Matching: The source and target in the program has data coming in various
formats and to get the best result we had to process and clean the data by removing hyphens, etc. We implemented the code to detect camel casing and whitespace. After that, we used similarity formula to derive similarity.
3. WordNet : If the score is too less, we have used wordnet API(JAWS) to detect the syntax of the word. After we obtain similar words of each source and target, we calculate the similarity using cosine similarity by constructing two vectors for each source and target. We used bag of words model too in this.
• For Property Matching
1. For Property, we took the domain and range of each property, and then applied all
the techniques applied above. This is because even if the strings matched, they may
be different property based on the ontology being matched.
2. For Data Property, we are only checking ranges for source and target nodes
ontology.
￼￼
￼
Precision : - 82.3
Recall: - ￼49.6
F-Measure: - 58.1
￼
After application of these techniques, we improved the F-Measure. Our aim was to increase the F-Measure score while keeping the precision as high as possible. This is because we estimate that our algorithm could be included in other techniques.
New Mappings found after application of our algorithms :
￼
Precision : - 86.2
Recall￼: - 57.9
F-Measure: - 67.8

￼Incorrect Mapping in the previous algorithm : Source : Paper Target : Paper
Dependencies :
1. Please download Wordnet and include the path. This has been highlighted in the code.
2. For running the application, Please replace MyMatcher.java from an existing project and
add our MyMatcher. The ontologies can be added to the src folder.

# AgreementMaker Ontology Matching System

AgreementMaker is an ontology matching system was started by the ADVIS Laboratory
at the University of Illinois at Chicago, under the supervision of [Professor Isabel F. Cruz](http://www.cs.uic.edu/Cruz/).

It has competed multiple times in the [Ontology Alignment Evaluation Initiative](http://oaei.ontologymatching.org/) 
and presented impressive results.

The currently supported version of AgreementMaker is under the [AgreementMaker-OSGi](AgreementMaker-OSGi) directory.

# Getting Started

Please read the [wiki on GitHub](https://github.com/agreementmaker/agreementmaker/wiki) for information on how to run AgreementMaker.
