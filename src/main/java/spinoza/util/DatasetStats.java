package spinoza.util;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DatasetStats {

	public static void main(String[] args) throws IOException {
		BabelNet bn = BabelNet.getInstance();
		String datasetDir = args[0];
		File entriesFile = new File(datasetDir, "entries.txt");
		File statsFile = new File(datasetDir, "stats.txt");
		if (statsFile.exists()) {
			System.err.printf("Statistics file already exists: %s", 
					statsFile.getAbsolutePath());
			System.exit(1);
		}
		
		int wordNetCount = 0;
		int wikiCount = 0;
		int wordNetWikiCount = 0;
		int otherCount = 0;
		int nonbabelnetCount = 0;
		
		int count = 0;
		try (FileReader f = new FileReader(entriesFile);
				BufferedReader in = new BufferedReader(f);
				FileWriter f2 = new FileWriter(new File(datasetDir, "wordnet.txt"));
				PrintWriter wn = new PrintWriter(f2, true);
				FileWriter f3 = new FileWriter(new File(datasetDir, "wikipedia.txt"));
				PrintWriter wiki = new PrintWriter(f3, true);) {
			System.err.print("Counting.");
			String offset;
			while ((offset = in.readLine()) != null) {
				if (!offset.startsWith("bn:")) {
					nonbabelnetCount++;
					continue;
				}
				BabelSynset synset = bn.getSynsetFromId(offset);
				if (synset == null) {
					System.err.println("Synset not found: " + offset);
					continue;
				}
				switch (synset.getSynsetSource()) {
				case WN:
					wordNetCount++;
					wn.println(offset);
					break;
				case WIKI:
					wikiCount++;
					wiki.println(offset);
					break;
				case WIKIWN:
					wordNetWikiCount++;
					wn.println(offset);
					wiki.println(offset);
					break;
				default:
					otherCount++;
					break;
				}
				count++;
				if (count % 1000 == 0) System.err.print(".");
			}
			System.err.println(" Done.");
		}
		
		try (PrintWriter out = new PrintWriter(statsFile)) {
			out.printf("WordNet: %d\n", wordNetCount);
			out.printf("Wiki: %d\n", wikiCount);
			out.printf("WordNet+Wiki: %d\n", wordNetWikiCount);
			out.printf("Other: %d\n", otherCount);
			out.printf("Non-BabelNet: %d\n", nonbabelnetCount);
		}
	}
	
}
