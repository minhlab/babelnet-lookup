package spinoza.blookup;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.BabelSenseSource;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.jlt.util.Language;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import edu.mit.jwi.item.IPointer;
import edu.mit.jwi.item.POS;

public class BabelNetRequestHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(BabelNetRequestHandler.class);

    private static final Pattern TEXT_REQUESTS = Pattern
            .compile("^/text/([^/]+)/([^/]+)$");
    private static final Pattern TEXT_NON_REDIRECT_REQUESTS = Pattern
            .compile("^/textnr/([^/]+)/([^/]+)/([^/]+)$");
    private static final Pattern WORDNET_REQUESTS = Pattern
            .compile("^/wordnet/([^/]+)$");
    private static final Pattern WIKIPEDIA_REQUESTS = Pattern
            .compile("^/wikipedia/([^/]+)(?:/([^/]))$");
    private static final Pattern RELATED_SYNSET_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/related$");
    private static final Pattern SENSES_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/senses(?:/(\\w+))$");
    private static final Pattern DBPEDIA_URI_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/dbpedia_uri(?:/(\\w+))$");

    private BabelNet bn = BabelNet.getInstance();

    public synchronized void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LOGGER.debug("Target: " + target);
        response.setContentType("text/plain");
        boolean handled = handleWordNetRequest(target, response) || 
        		handleTextRequest(target, response) ||
        		handleTextNonRedirectRequest(target, response) ||
                handleWikipediaRequest(target, response) || 
                handleRelatedSynsetRequest(target, response) ||
                handleSensesRequest(target, response) ||
                handleDBpediaRequest(target, response);
        baseRequest.setHandled(handled);
        // response.sendError(404);
    }

    private boolean handleWikipediaRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = WIKIPEDIA_REQUESTS.matcher(target);
        if (matcher.find()) {
            String title = matcher.group(1);
            POS pos = POS.getPartOfSpeech(matcher.group(2).charAt(0));
            LOGGER.debug("Wikipedia title: " + title + ", POS: " + pos);
            List<BabelSynset> synsets = bn.getSynsetsFromWikipediaTitle(
            		Language.EN, title, pos);
            if (synsets == null || synsets.isEmpty()) {
	            synsets = bn.getSynsets(Language.EN, title, pos);
            }
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleWordNetRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = WORDNET_REQUESTS.matcher(target);
        if (matcher.find()) {
            String offset = matcher.group(1);
            LOGGER.debug("WordNet offset: " + offset);
            List<BabelSynset> synsets = bn.getSynsetsFromWordNetOffset(offset);
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }


    private boolean handleTextRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = TEXT_REQUESTS.matcher(target);
        if (matcher.find()) {
            String langId = matcher.group(1);
            String query = matcher.group(2);
            Language lang = Language.valueOf(langId.toUpperCase());
			List<BabelSynset> synsets = bn.getSynsets(lang, query);
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }


    private boolean handleTextNonRedirectRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = TEXT_NON_REDIRECT_REQUESTS.matcher(target);
        if (matcher.find()) {
            String langId = matcher.group(1);
            String posId = matcher.group(2);
            String query = matcher.group(3);
            Language lang = Language.valueOf(langId.toUpperCase());
            POS pos = POS.getPartOfSpeech(posId.charAt(0));
			List<BabelSynset> synsets = bn.getSynsets(lang, query, pos, false,
					BabelSenseSource.values());
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleRelatedSynsetRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = RELATED_SYNSET_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynsetFromId(id);
            if (synset != null) {
                Map<IPointer, List<BabelSynset>> relatedMap = synset.getRelatedMap();
                for (Entry<IPointer, List<BabelSynset>> entry : relatedMap
                        .entrySet()) {
                    for (BabelSynset relatedSynset : entry.getValue()) {
                        IPointer pointer = entry.getKey();
//                        response.getWriter().format("%s\t%s\t%s\n",
//                                pointer.getSymbol(), relatedSynset.getId(),
//                                pointer.getName()); // name is for humans
                        response.getWriter().format("%s\t%s\n",
                                pointer.getSymbol(), relatedSynset.getId());
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSensesRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = SENSES_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            String langId = matcher.group(2);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynsetFromId(id);
            if (synset != null) {
            	List<BabelSense> senses;
            	if (langId == null || langId.isEmpty()) {
					senses = synset.getSenses();
				} else {
					senses = synset.getSenses(Language.valueOf(langId.toUpperCase()));
				}
                for (BabelSense sense : senses) {
                    response.getWriter().format("%s\t%s\t%s\t%s\n",
                            sense.getLemma(), sense.getPOS(),
                            sense.getLanguage(), sense.getSource());
                }
                return true;
            }
        }
        return false;
    }
    
    private boolean handleDBpediaRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = DBPEDIA_URI_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            String langId = matcher.group(2);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynsetFromId(id);
            if (synset != null) {
            	List<String> uris;
            	if (langId == null || langId.isEmpty()) {
					uris = synset.getDBPediaURIs();
				} else {
					uris = synset.getDBPediaURIs(Language.valueOf(langId.toUpperCase()));
				}
                for (String uri : uris) {
                    response.getWriter().println(uri);
                }
                return true;
            }
        }
        return false;
    }
}
