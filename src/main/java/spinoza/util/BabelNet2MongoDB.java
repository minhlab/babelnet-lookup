package spinoza.util;

import java.util.Set;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.iterators.BabelSynsetIterator;
import it.uniroma1.lcl.jlt.util.Language;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class BabelNet2MongoDB {
	
	private static final String COLL_NAME = "name-coll";
	private static final String DB_NAME = "semsig-db";

	public static void main(String[] args) throws Exception {
		BabelNet bn = BabelNet.getInstance();
		MongoClient mongoClient = new MongoClient("localhost");
		DB db = mongoClient.getDB(DB_NAME);
		DBCollection coll = db.getCollection(COLL_NAME);

		System.err.format("Dropping collection %s... ", COLL_NAME);
		coll.drop();
		System.err.println("Done.");
		
		System.err.println("Started importing names from BabelNet to MongoDB");
		int count = 0;
		for (BabelSynsetIterator it = bn.getSynsetIterator(); it.hasNext();) {
			BabelSynset synset = (BabelSynset) it.next();
			String id = synset.getId().replaceAll("bn:0*", "");
			Set<String> senseSet = Sets.newHashSet();
			for (BabelSense sense : synset.getSenses(Language.EN)) {
				senseSet.add(sense.getLemma().replaceAll("_", " "));
			}
			BasicDBList senses = new BasicDBList(); 
			senses.addAll(senseSet);
			BasicDBObject doc = new BasicDBObject("_id", id).append("senses", senses);
			coll.insert(doc);
			if (++count % 10000 == 0) {
				System.err.println(count + " ...");
			}
		}
		System.err.println("Finished importing names from BabelNet to MongoDB");
	}
}
