package nlp.assignments.tagger;

import java.util.List;

import nlp.util.Counter;
import nlp.util.CounterMap;

public class TrigramMEMMScorer implements LocalTrigramScorer {

    MaxEntModel maxEntModel = new MaxEntModel();
    CounterMap<String, String> wordtotags = new CounterMap<String, String>();
    
    public TrigramMEMMScorer(String featureFile) {
        maxEntModel.initializeForTraining(featureFile);
    }
    
    private Datum extractFeatures(LocalTrigramContext local) {
        Datum d = new Datum();
        d.addFV("p2", local.getPreviousPreviousWord() + ":" + local.getPreviousPreviousTag());
        d.addFV("pt2", local.getPreviousPreviousTag());
        d.addFV("p1", local.getPreviousWord() + ":" + local.getPreviousTag());
        d.addFV("pt1", local.getPreviousTag());
        d.addFV("cw", local.getCurrentWord());
        d.addFV("wf", wordFeature(local.getCurrentWord()));
        if (local.getPosition() > 0) {
            d.addFV("pwf", wordFeature(local.getPreviousWord()));
        }
        if (local.getPosition() > 1) {
            d.addFV("pwf2", wordFeature(local.getPreviousWord()));
        }
        for (int pre = 0; pre < 4; pre++) {
            String prefix = getPrefix(local.getCurrentWord(), pre);
            if (prefix != null) {
                d.addFV("prefix" + String.valueOf(pre), prefix);
            }
        }
        for (int suf = 0; suf < 4; suf++) {
            String suffix = getSuffix(local.getCurrentWord(), suf);
            if (suffix != null) {
                d.addFV("suffix" + String.valueOf(suf), suffix);
            }
        }
        return d;
    }
    
    private String getPrefix(String word, int p) {
        if (word.length() >= p) {
            return word.substring(0, p);
        } else {
            return null;
        }
    }
    
    private String getSuffix(String word, int p) {
        if (word.length() >= p) {
            return word.substring(word.length() - p, word.length());
        } else {
            return null;
        }
    }

    private String wordFeature (String word) {
        int len = word.length();
        boolean allDigits = true;
        boolean allCaps = true;
        boolean initCap = true;
        boolean allLower = true;
        boolean hyphenated = true;
        boolean abbrev = true;
        for (int i=0; i<len; i++) {
            char c = word.charAt(i);
            if (!Character.isDigit(c)) allDigits = false;
            if (!Character.isUpperCase(c)) allCaps = false;
            if (!Character.isLowerCase(c)) allLower = false;
            if (!(Character.isLetter(c) || c == '-')) hyphenated = false;
            if (!(Character.isLetter(c) || c == '.')) abbrev = false;
            if ((i == 0 && !Character.isUpperCase(c)) ||
                (i > 0  && !Character.isLowerCase(c))) initCap = false;
        }
        if (allDigits) {
            if (len == 2) {
                return "twoDigitNum";
            } else if (len == 4) {
                return "fourDigitNum";
            } else {
                return "otherNum";
            }
        } else if (allCaps) {
            return "allCaps";
        } else if (initCap) {
            return "initCap";
        } else if (allLower) {
            return "lowerCase";
            // any mix of letters and periods counts as an abbrev
        } else if (abbrev) {
            return "abbrev";
            // for POS
        } else if (hyphenated) {
            return "hyphenated";
        } else return "other";
    }
    
    @Override
    public Counter<String> getLogScoreCounter(
            LocalTrigramContext localTrigramContext) {
        
        Datum d = extractFeatures(localTrigramContext);
        double[] probs = maxEntModel.getOutcomeProbabilities(d);
        Counter<String> logCounter = new Counter<String>();

        String cur = localTrigramContext.getCurrentWord();
        if (wordtotags.containsKey(cur)) {
            Counter<String> tagCounter = wordtotags.getCounter(cur);
            for (int i = 0; i < probs.length; i++) {
                if (tagCounter.containsKey(maxEntModel.getOutcome(i))) {
                    logCounter.setCount(maxEntModel.getOutcome(i), Math.log(probs[i]));
                }
            }
        } else {
            for (int i = 0; i < probs.length; i++) {
                logCounter.setCount(maxEntModel.getOutcome(i), Math.log(probs[i]));
            }
        }
        return logCounter;
    }

    @Override
    public void train(List<LabeledLocalTrigramContext> localTrigramContexts) {
        System.out.println("Training... ");
        for (LabeledLocalTrigramContext llocal : localTrigramContexts) {
            Datum d = extractFeatures(llocal);
            d.setOutcome(llocal.getCurrentTag());
            maxEntModel.addEvent(d);
            wordtotags.incrementCount(llocal.getCurrentWord(), llocal.getCurrentTag(), 1);
        }
        System.out.println("Build model.. ");
        maxEntModel.buildModel();
        System.out.println("Train done.");
    }

    @Override
    public void validate(List<LabeledLocalTrigramContext> localTrigramContexts) {
    }

}
