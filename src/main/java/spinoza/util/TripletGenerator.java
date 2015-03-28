package spinoza.util;

import it.uniroma1.lcl.babelnet.BabelCategory;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetSource;
import it.uniroma1.lcl.babelnet.iterators.BabelSynsetIterator;
import it.uniroma1.lcl.jlt.util.Language;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

import edu.mit.jwi.item.IPointer;

public class TripletGenerator {

	public static void main(String[] args) throws IOException {
		CommandLine cmd = parseOptions(args);
    	int queryDelay = Integer.parseInt(cmd.getOptionValue("qdms", "0"));
    	String sparqlEndPoint = cmd.getOptionValue("sparql", "http://dbpedia.org/sparql");
        if (cmd.hasOption("wn")) {
        	extractWordNetRelationships(BabelNet.getInstance());
		}
        if (cmd.hasOption("r")) {
        	extractRelatedRelationships(BabelNet.getInstance(),
        			cmd.getOptionValue("from", ""));
		}
        if (cmd.hasOption("c")) {
        	extractCategories(BabelNet.getInstance());
        }
        if (cmd.hasOption("t")) {
        	extractDBpediaTypes(BabelNet.getInstance().getSynsetIterator(), 
        			sparqlEndPoint, queryDelay);
        }
        if (cmd.hasOption("o")) {
        	extractOntology(sparqlEndPoint, queryDelay);
        }
		System.err.println("Done.");
	}

	/**
	 * Extract WordNet relations that were used in Bordes et al. (2013).
	 * 
	 * @param bn
	 * @throws IOException
	 * 
	 * References:
	 * 
	 * Bordes, A., Glorot, X., Weston, J., & Bengio, Y. (2013). A
	 * semantic matching energy function for learning with
	 * multi-relational data. Machine Learning, 94(2), 233–259.
	 * doi:10.1007/s10994-013-5363-6
	 */
	private static void extractWordNetRelationships(BabelNet bn) throws IOException {
		ImmutableSet<String> wordNetRelations = ImmutableSet.of(
				 "@", "@i", "~", "~i", "+", "#m", "#p", "%m", "%p", 
				 "^", "=", ";r", "-r", ";c", "-c", ";u", "-u", "$");
		for (BabelSynsetIterator it = bn.getSynsetIterator(); it.hasNext();) {
			BabelSynset synset = (BabelSynset) it.next();
			if (synset.getSynsetSource() == BabelSynsetSource.WN ||
					synset.getSynsetSource() == BabelSynsetSource.WIKIWN) {
				Map<IPointer, List<BabelSynset>> relatedMap = synset.getRelatedMap();
				for (Entry<IPointer, List<BabelSynset>> entry : relatedMap.entrySet()) {
					IPointer pointer = entry.getKey();
					if (wordNetRelations.contains(pointer.getSymbol())) {
		                for (BabelSynset relatedSynset : entry.getValue()) {
				                System.out.printf("%s\t%s\t%s\n", synset.getId(),
				                		pointer.getSymbol(), relatedSynset.getId());
		                }
					} // end check relationship type
	            } // end for
			} // end check source
		}
	}

	private static void extractRelatedRelationships(BabelNet bn, String from) throws IOException {
		int count = 0;
		BabelSynsetIterator it = bn.getSynsetIterator();
		from = from.trim();
		if (!from.isEmpty()) {
			System.err.println("Skipping...\n");
			int skippingCount = 0;
			while (it.hasNext()) {
				BabelSynset synset = (BabelSynset) it.next();
				if (from.equals(synset.getId())) {
					break;
				}
				skippingCount++;
				if (skippingCount % 10000 == 0) {
					System.err.println(skippingCount + "...");
				}
			}
			System.err.println("Skipped " + skippingCount + " synsets.\n");
		}
		while (it.hasNext()) {
			BabelSynset synset = (BabelSynset) it.next();
			Map<IPointer, List<BabelSynset>> relatedMap = synset.getRelatedMap();
			for (Entry<IPointer, List<BabelSynset>> entry : relatedMap
                    .entrySet()) {
	                for (BabelSynset relatedSynset : entry.getValue()) {
			                IPointer pointer = entry.getKey();
			                System.out.printf("%s\t%s\t%s\n", synset.getId(),
			                		pointer.getSymbol(), relatedSynset.getId());
			                count++;
			                if (count % 10000 == 0) {
			                	System.err.println(count + "...");
			                }
	                }
            }
		}
		System.err.println("Successfully finished!\n");
	}

	private static void extractCategories(BabelNet bn) throws IOException {
		for (BabelSynsetIterator it = bn.getSynsetIterator(); it.hasNext();) {
			BabelSynset synset = (BabelSynset) it.next();
			for (BabelCategory cat : synset.getCategories(Language.EN)) {
				System.out.printf("%s\t%s\t%s\n", synset.getId(),
                		"spinoza:belong-to", "wiki:cat:" + cat.getCategory());
			}
		}
	}
	
	static void extractDBpediaTypes(Iterator<BabelSynset> it, 
			String sparqlEndPoint, int queryDelay) {
		System.err.println("Deprecated: Download and extract information from" +
							"http://wiki.dbpedia.org/Downloads2014 instead");
		
		while (it.hasNext()) {
			BabelSynset synset = (BabelSynset) it.next();
			List<String> uris = synset.getDBPediaURIs(Language.EN);
			if (uris.size() > 1) {
				System.err.printf("Found more than one DBpedia URIs for %s\n", 
						synset.getId());
			}
			for (String uri : uris) {
				uri = sanitize(uri);
				try {
					for (String type : queryDBpediaTypes(uri, sparqlEndPoint)) {
						System.out.printf("%s\t%s\t%s\n", synset.getId(), 
								"rdf:type", abbr(type));
					}
					if (queryDelay > 0) {
						// public end points enforce restriction on how many 
						// queries are submitted or how heavy a query can be
						Thread.sleep(queryDelay); // give it a break
					}
				} catch (QueryExceptionHTTP ex) {
					System.err.printf("HTTP error %d for %s (%s)\n", 
							ex.getResponseCode(), uri, synset.getId());
				} catch (Exception ex) {
					// this is a very time-consuming process, I don't want it to be 
					// ruined by any exception
					System.err.printf("Exception while fetching %s (%s): %s\n", 
							uri, synset.getId(), ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * BabelNet saves DBpedia URIs in an erroneous manner which resulted in 
	 * a lot of exceptions in my program. This method fixes that.
	 * 
	 * Made available for testing.
	 * @param uri
	 * @return
	 */
	static String sanitize(String uri) {
		uri = uri.replace("DBpedia.org", "dbpedia.org"); // fix case
		int lastSlashPos = uri.lastIndexOf('/');
		String lastPart = uri.substring(lastSlashPos+1);
		try {
			lastPart = URLEncoder.encode(lastPart, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println("You don't support UTF-8? Are you serious?");
			e.printStackTrace();
		}
		uri = uri.substring(0, lastSlashPos+1) + lastPart;
		return uri;
	}

	static String abbr(String type) {
		type = type.replace("http://dbpedia.org/ontology/", "dbpedia-owl:");
		type = type.replace("http://www.w3.org/2002/07/owl#", "owl:");
		return type;
	}
	
	/**
	 * Made available for testing purpose
	 * @param uri
	 * @param sparqlEndPoint
	 * @return
	 */
	static List<String> queryDBpediaTypes(String uri, String sparqlEndPoint) {
	    String sparqlStr= String.format(
	    		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
	    		"select * where {<%s> rdf:type ?type \n" +
	    		"FILTER(STRSTARTS(STR(?type), \"http://dbpedia.org/ontology\")) \n" +
				"} LIMIT 100", uri);

		QueryExecution qexec = null;
		try {
			Query query = QueryFactory.create(sparqlStr);
			qexec = QueryExecutionFactory.sparqlService(sparqlEndPoint, query);
			
			List<String> types = new LinkedList<String>();
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution solution = results.nextSolution();
				types.add(solution.getResource("type").getURI());
			}
			return types;
		} finally {
			if (qexec != null) qexec.close();
		}
	}
	
	private static void extractOntology(String sparqlEndPoint, int queryDelay) {
		int limit = 100;
		int offset = 0;
		while (true) {
			try {
				List<String[]> pairs = queryOntology(sparqlEndPoint, limit, offset);
				if (pairs.isEmpty()) {
					break;
				}
				for (String[] pair : pairs) {
					System.out.printf("%s\t%s\t%s\n", abbr(pair[0]), 
							"rdfs:subClassOf", abbr(pair[1]));
				}
				offset += limit;
				if (queryDelay > 0) {
					// public end points enforce restriction on how many 
					// queries are submitted or how heavy a query can be
					Thread.sleep(queryDelay); // give it a break
				}
			} catch (QueryExceptionHTTP ex) {
				System.err.printf("HTTP error %d at offset %d\n", ex.getResponseCode(), offset);
			} catch (Exception ex) {
				// this is a very time-consuming process, I don't want it to be 
				// ruined by any exception
				System.err.printf("Exception at offset %d: %s\n", offset, ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Made available for testing purpose
	 * @param sparqlEndPoint
	 * @param limit
	 * @param offset
	 * @return
	 */
	static List<String[]> queryOntology(String sparqlEndPoint, Object limit, int offset) {
		String queryTemplate = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "select * where {?child rdfs:subClassOf ?parent \n" 
				+ "FILTER(STRSTARTS(STR(?child), \"http://dbpedia.org/ontology\"))} \n"
				+ "LIMIT %d OFFSET %d";
		QueryExecution qexec = null;
		try {
			String queryStr = String.format(queryTemplate, limit, offset);
//			System.err.println(queryStr);
			Query query = QueryFactory.create(queryStr);
			qexec = QueryExecutionFactory.sparqlService(sparqlEndPoint, query);
			ResultSet results = qexec.execSelect();
			List<String[]> pairs = new LinkedList<String[]>();
			while (results.hasNext()) {
				QuerySolution solution = results.nextSolution();					
				String child = solution.getResource("child").getURI();
				String parent = solution.getResource("parent").getURI();
				pairs.add(new String[] {child, parent});
			}
			return pairs;
		} finally {
			if (qexec != null) qexec.close();
		}
	}
	
    private static CommandLine parseOptions(String[] args) {
        Options options = new Options();
        try {
        	options.addOption("wn", false, "WordNet only");
        	options.addOption("r", false, "related synsets");
        	options.addOption("from", true, "starting from");
        	options.addOption("c", false, "categories");
        	options.addOption("t", false, "DBpedia ontology types");
        	options.addOption("o", false, "DBpedia ontology hierarchy itself");
        	options.addOption("qdms", "query-delay-ms", true, "Delay after each query to SPARQL endpoint");
        	options.addOption("sparql", true, "URI to SPARQL endpoint (default to DBpedia)");
            CommandLine cmd = new PosixParser().parse(options, args);
			if (!cmd.hasOption("wn") && !cmd.hasOption("r")
					&& !cmd.hasOption("c") && !cmd.hasOption("t")
					&& !cmd.hasOption("o")) {
            	System.err.println("At least one type of triplets must be enabled.");
            	printHelpAndExit(options);
            }
			return cmd;
        } catch (ParseException e) {
            printHelpAndExit(options);
            return null;
        }
    }

	private static void printHelpAndExit(Options options) {
		new HelpFormatter().printHelp("triplet-generator [options]", options);
		System.exit(1);
	}
}
