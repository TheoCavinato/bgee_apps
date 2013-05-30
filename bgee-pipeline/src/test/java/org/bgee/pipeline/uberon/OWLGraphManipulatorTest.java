package org.bgee.pipeline.uberon;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.TestAncestor;
import org.junit.Before;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;

/**
 * Test the functionalities of {@link org.bgee.pipeline.uberon.OWLGraphManipulator}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13, May 2013
 * @since Bgee 13
 *
 */
public class OWLGraphManipulatorTest extends TestAncestor
{
    private final static Logger log = 
    		LogManager.getLogger(OWLGraphManipulatorTest.class.getName());
    /**
     * The <code>OWLGraphWrapper</code> used to perform the test. 
     */
    private OWLGraphManipulator graphManipulator;
	
	/**
	 * Default Constructor. 
	 */
	public OWLGraphManipulatorTest()
	{
		super();
	}
	@Override
	protected Logger getLogger() {
		return log;
	}
	
	/**
	 * Load the (really basic) ontology <code>/ontologies/OWLGraphManipulatorTest.obo</code> 
	 * into {@link #graphWrapper}.
	 * It is loaded before the execution of each test, so that a test can modify it 
	 * without impacting another test.
	 *  
	 * @throws OWLOntologyCreationException 
	 * @throws OBOFormatParserException
	 * @throws IOException
	 * 
	 * @see #graphWrapper
	 */
	@Before
	public void loadTestOntology() 
			throws OWLOntologyCreationException, OBOFormatParserException, IOException
	{
		log.debug("Wrapping test ontology into OWLGraphManipulator...");
		ParserWrapper parserWrapper = new ParserWrapper();
        OWLOntology ont = parserWrapper.parse(
        		this.getClass().getResource("/ontologies/OWLGraphManipulatorTest.obo").getFile());
    	this.graphManipulator = new OWLGraphManipulator(new OWLGraphWrapper(ont));
		log.debug("Done wrapping test ontology into OWLGraphManipulator.");
	}
	
	
	//***********************************************
	//    RELATION REDUCTION AND RELATED TESTS
	//***********************************************
	/**
	 * Test the functionality of {@link OWLGraphManipulator.reduceRelations()}.
	 */
	@Test
	public void shouldReduceRelations()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		int relsRemoved = this.graphManipulator.reduceRelations();
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//3 relations should have been removed
		assertEquals("Incorrect number of relations removed", 3, relsRemoved);
		//check that it corresponds to the number of axioms removed
		assertEquals("Returned value does not correspond to the number of axioms removed", 
				relsRemoved, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations removed correspond to the proper relations to remove
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		OWLObjectProperty partOf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("BFO:0000050");
		OWLObjectProperty overlaps = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002131");
		
		//FOO:0003 part_of FOO:0001 redundant 
		//(FOO:0003 in_deep_part_of FOO:0004  part_of FOO:0002 part_of FOO:0001)
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0003");
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, root, partOf, Quantifier.SOME, ont);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0004 overlaps FOO:0001 redundant
		//(FOO:0004 part_of FOO:0002 part_of FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0004");
		checkEdge = new OWLGraphEdge(source, root, overlaps, Quantifier.SOME, ont);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0014 is_a FOO:0001 redundant
		//(FOO:0014 is_a FOO:0006 is_a FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		checkEdge = new OWLGraphEdge(source, root);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
	}
	/**
	 * Test the functionality of {@link OWLGraphManipulator.reducePartOfAndSubClassOfRelations()}.
	 */
	@Test
	public void shouldReducePartOfAndSubClassOfRelations()
	{
		//get the original number of axioms
	    int axiomCountBefore = 
	    		this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		int relsRemoved = this.graphManipulator.reducePartOfAndSubClassOfRelations();
		
		//get the number of axioms after removal
		int axiomCountAfter = 
				this.graphManipulator.getOwlGraphWrapper().getSourceOntology().getAxiomCount();
				
		//3 relations should have been removed
		assertEquals("Incorrect number of relations removed", 3, relsRemoved);
		//check that it corresponds to the number of axioms removed
		assertEquals("Returned value does not correspond to the number of axioms removed", 
				relsRemoved, axiomCountBefore - axiomCountAfter);
		
		//Check that the relations removed correspond to the proper relations to remove
		OWLOntology ont = this.graphManipulator.getOwlGraphWrapper().getSourceOntology();
		OWLDataFactory factory = this.graphManipulator.getOwlGraphWrapper().
				getManager().getOWLDataFactory();
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		
		//FOO:0005 is_a FOO:0001 redundant 
		//(FOO:0005 part_of FOO:0002 part_of FOO:0001)
		OWLClass source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0005");
		OWLGraphEdge checkEdge = new OWLGraphEdge(source, root);
		OWLAxiom axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0014 is_a FOO:0001 redundant
		//(FOO:0014 is_a FOO:0002 part_of FOO:0001)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014");
		checkEdge = new OWLGraphEdge(source, root);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
		
		//FOO:0013 is_a FOO:0001 redundant
		//(FOO:0013 part_of FOO:0001, equivalent direct outgoing edge)
		source = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0013");
		checkEdge = new OWLGraphEdge(source, root);
		axiom = factory.getOWLSubClassOfAxiom(source, 
				(OWLClassExpression) this.graphManipulator.getOwlGraphWrapper().
				edgeToTargetExpression(checkEdge));
		assertFalse("Incorrect relation removed", ont.containsAxiom(axiom));
	}

	/**
	 * Test the functionalities of
	 * {@link OWLGraphManipulator#combinedPropertyPairOverSuperProperties(OWLQuantifiedProperty, OWLQuantifiedProperty)}.
	 */
	@Test
	public void shouldCombinedPropertyPairOverSuperProperties() 
			throws NoSuchMethodException, SecurityException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException
	{
		//try to combine a has_developmental_contribution_from 
		//and a transformation_of relation (one is a super property of the other, 
		//2 levels higher, interesting unit test)
		OWLObjectProperty transf = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("http://semanticscience.org/resource/SIO_000657");
		OWLQuantifiedProperty transfQp = 
				new OWLQuantifiedProperty(transf, Quantifier.SOME);
		OWLObjectProperty devCont = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("RO:0002254");
		OWLQuantifiedProperty devContQp = 
				new OWLQuantifiedProperty(devCont, Quantifier.SOME);
		
		//method to test is private, yet we want to unit test it
		Method method = this.graphManipulator.getClass().getDeclaredMethod(
				"combinePropertyPairOverSuperProperties", 
				new Class<?>[] {OWLQuantifiedProperty.class, OWLQuantifiedProperty.class});
		method.setAccessible(true);
		
		OWLQuantifiedProperty combine =  
				(OWLQuantifiedProperty) method.invoke(this.graphManipulator, 
						new Object[] {transfQp, devContQp});
		assertEquals("relations SIO:000657 and RO:0002254 were not properly combined " +
				"into RO:0002254", devContQp, combine);
		//combine in the opposite direction, just to be sure :p
		combine =  
				(OWLQuantifiedProperty) method.invoke(this.graphManipulator, 
						new Object[] {devContQp, transfQp});
		assertEquals("Reversing relations in method call generated an error", 
				devContQp, combine);
		
		//another test case: two properties where none is parent of the other one, 
		//sharing several common parents, only the more general one is transitive. 
		//as I couldn't find any suitable example, fake relations were created
		//in the test ontology: 
		//fake_rel3 and fake_rel4 are both sub-properties of fake_rel2, 
		//which is not transitive, but has the super-property fake_rel1 
		//which is transitive. fake_rel3 and fake_rel4 should be combined into fake_rel1.
		OWLObjectProperty fakeRel3 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("fake_rel3");
		OWLQuantifiedProperty fakeRel3Qp = 
				new OWLQuantifiedProperty(fakeRel3, Quantifier.SOME);
		OWLObjectProperty fakeRel4 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("fake_rel4");
		OWLQuantifiedProperty fakeRel4Qp = 
				new OWLQuantifiedProperty(fakeRel4, Quantifier.SOME);
		
		combine =  
				(OWLQuantifiedProperty) method.invoke(this.graphManipulator, 
						new Object[] {fakeRel3Qp, fakeRel4Qp});
		OWLObjectProperty fakeRel1 = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("fake_rel1");
		assertEquals("relations fake_rel3 and fake_rel4 were not properly combined " +
				"into fake_rel1", fakeRel1, combine.getProperty());
		//combine in the opposite direction, just to be sure :p
		combine =  
				(OWLQuantifiedProperty) method.invoke(this.graphManipulator, 
						new Object[] {fakeRel4Qp, fakeRel3Qp});
		assertEquals("Reversing relations in method call generated an error", 
				fakeRel1, combine.getProperty());
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#getSubPropertyReflexiveClosureOf(OWLObjectPropertyExpression)}. 
	 * This will allow to check all the "get sub-properties" method at once.
	 */
	@Test
	public void shouldGetSubPropertyReflexiveClosureOf() throws NoSuchMethodException, 
	    SecurityException, IllegalAccessException, IllegalArgumentException, 
	    InvocationTargetException
	{
		Method method = this.graphManipulator.getClass().getDeclaredMethod(
				"getSubPropertyReflexiveClosureOf", 
				new Class<?>[] {OWLObjectPropertyExpression.class});
		method.setAccessible(true);
		
		//let's try with the fake relations in the test ontology: 
		OWLObjectProperty fakeRel = this.graphManipulator.getOwlGraphWrapper().
				getOWLObjectPropertyByIdentifier("fake_rel1");
		
		@SuppressWarnings("unchecked")
		//this warning is here only because we use reflection to test a private method
		LinkedHashSet<OWLObjectPropertyExpression> subPropsReflexive = 
				(LinkedHashSet<OWLObjectPropertyExpression>) method.invoke(this.graphManipulator, 
				new Object[] {fakeRel});
		
		//we should have 4 relations (fake_rel1 to 4)
		assertEquals("Incorrect number of sub-properties", 4, subPropsReflexive.size());
		
		//now check the order
		int count = 0;
		for (OWLObjectPropertyExpression prop: subPropsReflexive) {
			//first rel should be fake_rel1 (reflexive method)
			if (count == 0) {
				assertEquals("Incorrect order of sub-properties, 1st relation", 
						fakeRel, prop);
			} else if (count == 1) {
				OWLObjectProperty fakeRel2 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel2");
				assertEquals("Incorrect order of sub-properties, 2nd relation", 
						fakeRel2, prop);
			} else if (count == 2 || count == 3) {
				OWLObjectProperty fakeRel3 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel3");
				OWLObjectProperty fakeRel4 = this.graphManipulator.getOwlGraphWrapper().
						getOWLObjectPropertyByIdentifier("fake_rel4");
				assertTrue("Incorrect order of sub-properties, 3rd or 4th relation", 
						(fakeRel3.equals(prop) || fakeRel4.equals(prop)));
			} else {
				//should not be reached
				throw new AssertionError("Incorrect number of sub-properties");
			}
			count++;
		}
	}
	
	
	//***********************************************
	//    RELATION FILTERING AND REMOVAL TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>false</code>.
	 */
	@Test
	public void shouldFilterRelations()
	{
		//filter relations to keep only is_a, part_of and develops_from
		//5 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
				false, 5, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>true</code>.
	 */
	@Test
	public void shouldFilterRelationsWithSubRel()
	{
		//filter relations to keep is_a, part_of, develops_from, 
		//and their sub-relations.
		//3 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
				true, 3, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * when filtering a relation with a non-OBO-style ID (in this method, 
	 * <code>http://semanticscience.org/resource/SIO_000657</code>).
	 */
	@Test
	public void shouldFilterRelationsWithNonOboId()
	{
		//filter relations to keep only is_a and transformation_of relations
		//13 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("http://semanticscience.org/resource/SIO_000657"), 
				true, 13, true);
	}	
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#filterRelations(Collection, boolean)} 
	 * when filtering all relations but is_a.
	 */
	@Test
	public void shouldFilterAllRelations()
	{
		//filter relations to keep only is_a relations
		//14 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList(""), 
				true, 14, true);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>false</code>.
	 */
	@Test
	public void shouldRemoveRelations()
	{
		//remove part_of and develops_from relations
		//9 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("BFO:0000050", "RO:0002202"), 
			false, 9, false);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with the <code>boolean</code> parameters set to <code>true</code>.
	 */
	@Test
	public void shouldRemoveRelationsWithSubRel()
	{
		//remove develops_from relations and sub-relations
		//2 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList("RO:0002202"), 
			true, 2, false);
	}
	/**
	 * Test the functionalities of 
	 * {@link org.bgee.pipeline.uberon.OWLGraphManipulator#removeRelations(Collection, boolean)} 
	 * with an empty list of relations to remove, to check that it actually removed nothing.
	 */
	@Test
	public void shouldRemoveNoRelation()
	{
		//remove nothing
		//0 relations should be removed
		this.shouldFilterOrRemoveRelations(Arrays.asList(""), 
			true, 0, false);
	}
	/**
	 * Method to test the functionalities of 
	 * {@link OWLGraphManipulator#filterRelations(Collection, boolean)} and 
	 * {@link OWLGraphManipulator#removeRelations(Collection, boolean)}
	 * with various configurations, called by the methods performing the actual unit test. 
	 * 
	 * @param rels 				corresponds to the first parameter of 
	 * 							the <code>filterRelations</code> or 
	 * 							<code>removeRelations</code> method.
	 * @param subRels			corresponds to the second parameter of 
	 * 							the <code>filterRelations</code> or 
	 * 							<code>removeRelations</code> method.
	 * @param expRelsRemoved 	An <code>int</code> representing the expected number 
	 * 							of relations removed
	 * @param filter 			A <code>boolean</code> defining whether the method tested is 
	 * 							<code>filterRelations</code>, or <code>removeRelations</code>. 
	 * 							If <code>true</code>, the method tested is 
	 * 							<code>filterRelations</code>.
	 */
	private void shouldFilterOrRemoveRelations(Collection<String> rels, 
			boolean subRels, int expRelsRemoved, boolean filter)
	{
		//get the original number of axioms
		int axiomCountBefore = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getAxiomCount();
		
		//filter relations to keep 
		int relRemovedCount = 0;
		if (filter) {
			relRemovedCount = this.graphManipulator.filterRelations(rels, subRels);
		} else {
			relRemovedCount = this.graphManipulator.removeRelations(rels, subRels);
		}
		//expRelsRemoved relations should have been removed
		assertEquals("Incorrect number of relations removed", expRelsRemoved, relRemovedCount);
		
		//get the number of axioms after removal
		int axiomCountAfter = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getAxiomCount();
		//check that it corresponds to the returned value
		assertEquals("The number of relations removed does not correspond to " +
				"the number of axioms removed", 
				axiomCountBefore - axiomCountAfter, relRemovedCount);
	}
	
	

	//***********************************************
	//    SUBGRAPH FILTERING AND REMOVAL TESTS
	//***********************************************
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#filterSubgraphs(Collection)}.
	 */
	@Test
	public void shouldFilterSubgraphs()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep 
		//(FOO:0011, FOO:0014).
		//All terms belonging to the subgraph to remove, except these common terms, 
		//should be removed.
		
		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				    .getSourceOntology().getClassesInSignature().size();
		
		//filter the subgraphs, we want to keep: 
		//FOO:0002 corresponds to term "A", root of the first subgraph to keep. 
		//FOO:0013 to "subgraph3_root".
		//FOO:0014 to "subgraph4_root_subgraph2" 
		//(both root of a subgraph to keep, and part of a subgraph to remove).
		//subgraph starting from FOO:0006 "subgraph2_root" will be removed, 
		//(but not FOO:0006 itself, because it is an ancestor of FOO:0014; 
		//if FOO:0014 was not an allowed root, then FOO:0006 would be removed)
		Collection<String> toKeep = new ArrayList<String>();
		toKeep.add("FOO:0002");
		toKeep.add("FOO:0013");
		toKeep.add("FOO:0014");
		int countRemoved = this.graphManipulator.filterSubgraphs(toKeep);
		
		//The test ontology is designed so that 5 classes should have been removed
		assertEquals("Incorrect number of classes removed", 5, countRemoved);
		
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
			    .getSourceOntology().getClassesInSignature().size();
		assertEquals("filterSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);
		
		//Test that the terms part of both subgraphs were not incorrectly removed.
		//Their IDs are FOO:0011 and FOO:0014, they have slighty different relations to the root
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0011"));
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014"));
		
		//now, we need to check that the relations FOO:0003 B is_a FOO:0001 root, 
		//FOO:0004 C part_of FOO:0001 root, FOO:0005 D is_a FOO:0001 root
		//have been removed (terms should be kept as it is part of a subgraph to keep, 
		//but the relations to the root are still undesired subgraphs, 
		//that should be removed)
		OWLClass root = 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0001");
		for (OWLGraphEdge incomingEdge: 
		    this.graphManipulator.getOwlGraphWrapper().getIncomingEdges(root)) {
			assertNotEquals("The relation FOO:0003 B is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0003", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
			assertNotEquals("The relation FOO:0004 C is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0004", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
			assertNotEquals("The relation FOO:0005 D is_a FOO:0001 root, " +
					"causing an undesired subgraph, was not correctly removed", 
					"FOO:0005", 
					this.graphManipulator.getOwlGraphWrapper().getIdentifier(
							incomingEdge.getSource()));
		}
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeSubgraphs(Collection, boolean)}, 
	 * with the <code>boolean</code> parameter set to <code>true</code>.
	 */
	@Test
	public void shouldRemoveSubgraphs()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep.
		//All terms belonging to the subgraph to remove, except these common terms, 
		//should be removed.

		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();

		//remove the subgraph
		Collection<String> toRemove = new ArrayList<String>();
		toRemove.add("FOO:0006");
		//add as a root to remove a term that is in the FOO:0006 subgraph, 
		//to check if the ancestors check will not lead to keep erroneously FOO:0007
		toRemove.add("FOO:0008");
		int countRemoved = this.graphManipulator.removeSubgraphs(toRemove, true);

		//The test ontology is designed so that 6 classes should have been removed
		assertEquals("Incorrect number of classes removed", 6, countRemoved);
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();
		assertEquals("removeSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);

		//Test that the terms part of both subgraphs, or part of independent subgraphs, 
		//were not incorrectly removed.
		//Their IDs are FOO:0011 and FOO:0014, they have slighty different relations to the root
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0011"));
		assertNotNull("A term part of both subgraphs was incorrectly removed", 
				this.graphManipulator.getOwlGraphWrapper().getOWLClassByIdentifier("FOO:0014"));
	}
	
	/**
	 * Test the functionalities of 
	 * {@link OWLGraphManipulator#removeSubgraphs(Collection, boolean)}, 
	 * with the <code>boolean</code> parameter set to <code>false</code>.
	 */
	@Test
	public void shouldRemoveSubgraphsAndSharedClasses()
	{
		//The test ontology includes several subgraphs, with 1 to be removed, 
		//and with two terms part of both a subgraph to remove and a subgraph to keep.
		//All terms belonging to the subgraph to remove, EVEN these common terms, 
		//should be removed.

		//first, let's get the number of classes in the ontology
		int classCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();

		//remove the subgraph
		Collection<String> toRemove = new ArrayList<String>();
		toRemove.add("FOO:0006");
		int countRemoved = this.graphManipulator.removeSubgraphs(toRemove, false);

		//The test ontology is designed so that 8 classes should have been removed
		assertEquals("Incorrect number of classes removed", 8, countRemoved);
		//test that these classes were actually removed from the ontology
		int newClassCount = this.graphManipulator.getOwlGraphWrapper()
				.getSourceOntology().getClassesInSignature().size();
		assertEquals("removeSubgraph did not return the correct number of classes removed", 
				classCount - newClassCount, countRemoved);
	}
}
