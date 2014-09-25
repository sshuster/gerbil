package org.aksw.gerbil.bat.annotator;

import it.acubelab.batframework.data.Annotation;
import it.acubelab.batframework.data.Mention;
import it.acubelab.batframework.problems.D2WSystem;
import it.acubelab.batframework.systemPlugins.TimingCalibrator;
import it.acubelab.batframework.utils.AnnotationException;
import it.uniroma1.lcl.babelfy.Babelfy;
import it.uniroma1.lcl.babelfy.Babelfy.AccessType;
import it.uniroma1.lcl.babelfy.Babelfy.Matching;
import it.uniroma1.lcl.babelfy.BabelfyKeyNotValidOrLimitReached;
import it.uniroma1.lcl.babelfy.data.BabelSynsetAnchor;
import it.uniroma1.lcl.jlt.util.Language;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.aksw.gerbil.bat.converter.DBpediaToWikiId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The BabelFy Annotator.
 * 
 * <p>
 * <i>Andrea Moro: "I recommend to use the maximum amount of available characters (3500) at each request (i.e., try to
 * put a document all together or split it in chunks of 3500 characters) both for scalability and performance
 * reasons."</i><br>This means, that we have to split up documents longer than 3500 characters. Unfortunately, BabelFy seems
 * to measure the length on the escaped text which means that every text could be three times longer than the unescaped
 * text. Thus, we have to set {@link #BABELFY_MAX_TEXT_LENGTH}={@value #BABELFY_MAX_TEXT_LENGTH}.
 * </p>
 */
public class BabelfyAnnotator implements D2WSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabelfyAnnotator.class);

    private static final int BABELFY_MAX_TEXT_LENGTH = 1000;

    private long calib = -1;
    private long lastTime = -1;
    private String key;

    public BabelfyAnnotator() {
        this("");
    }

    public BabelfyAnnotator(String key) {
        this.key = key;
    }

    public String getName() {
        return "BabelFy";
    }

    public long getLastAnnotationTime() {
        // FIXME @Didier: How this should work?
        if (calib == -1)
            calib = TimingCalibrator.getOffset(this);
        return lastTime - calib > 0 ? lastTime - calib : 0;
    }

    public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions)
            throws AnnotationException {
        if (text.length() > BABELFY_MAX_TEXT_LENGTH) {
            return solveD2WForLongTexts(text, mentions);
        }
        Babelfy bfy = Babelfy.getInstance(AccessType.ONLINE);
        HashSet<Annotation> annotations = Sets.newHashSet();
        try {
            it.uniroma1.lcl.babelfy.data.Annotation babelAnnotations = bfy.babelfy(key, text, Matching.EXACT,
                    Language.EN);
            for (BabelSynsetAnchor anchor : babelAnnotations.getAnnotations()) {
                List<String> uri = anchor.getBabelSynset().getDBPediaURIs(Language.EN);
                if ((uri != null) && (uri.size() > 0)) {
                    int id = DBpediaToWikiId.getId(uri.get(0));
                    annotations.add(new Annotation(anchor.getStart(), anchor.getEnd() - anchor.getStart(), id));
                }
            }
        } catch (BabelfyKeyNotValidOrLimitReached e) {
            LOGGER.error("The BabelFy Key is invalid or has reached its limit.", e);
            throw new AnnotationException("The BabelFy Key is invalid or has reached its limit: "
                    + e.getLocalizedMessage());
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Exception while requesting annotations from BabelFy. Returning empty Annotation set.", e);
        }
        return annotations;
    }

    private HashSet<Annotation> solveD2WForLongTexts(String text, HashSet<Mention> mentions) {
        List<String> chunks = splitText(text);
        HashSet<Annotation> annotations;
        annotations = solveD2W(chunks.get(0), mentions);

        HashSet<Annotation> tempAnnotations;
        int startOfChunk = 0;
        for (int i = 1; i < chunks.size(); ++i) {
            tempAnnotations = solveD2W(chunks.get(i), mentions);
            // We have to correct the positions of the annotations
            startOfChunk += chunks.get(i - 1).length();
            for (Annotation annotation : tempAnnotations) {
                annotations.add(new Annotation(annotation.getPosition() + startOfChunk, annotation.getLength(),
                        annotation.getConcept()));
            }
        }
        return annotations;
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<String>();
        int start = 0, end = 0, nextEnd = 0;
        // As long as we have to create chunks
        while ((nextEnd >= 0) && ((text.length() - nextEnd) > BABELFY_MAX_TEXT_LENGTH)) {
            // We have to use the next space, even it would be too far away
            end = nextEnd = text.indexOf(' ', start + 1);
            // Search for the next possible end this chunk
            while ((nextEnd >= 0) && ((nextEnd - start) < BABELFY_MAX_TEXT_LENGTH)) {
                end = nextEnd;
                nextEnd = text.indexOf(' ', end + 1);
            }
            // Add the chunk
            chunks.add(text.substring(start, end));
            start = end;
        }
        // Add the last chunk
        chunks.add(text.substring(start));
        return chunks;
    }
}