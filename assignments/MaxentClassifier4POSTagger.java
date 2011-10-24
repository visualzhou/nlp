package nlp.assignments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.corba.se.idl.PragmaEntry;

import nlp.classify.FeatureExtractor;
import nlp.classify.LabeledInstance;
import nlp.util.BoundedList;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class MaxentClassifier4POSTagger {

	public static class PosTaggerFeatureExtractor implements
			FeatureExtractor<String, String> {
		@Override
		public Counter<String> extractFeatures(String instance) {
			Counter<String> features = new Counter<String>();
			// suffix
			char[] characters = instance.toCharArray();
			List<String> charList = new ArrayList<String>();
			for (int i = 0; i < characters.length; i++) {
				charList.add(String.valueOf(characters[i]));
			}
			BoundedList<String> boundedString = new BoundedList<String>(
					charList, "<S>");

			// Suffix
			for (int i = 1; i < 4; i++) {
				String str = "SUFFIX-";
				for (int j = characters.length - i; j < characters.length; j++) {
					str += boundedString.get(j);
				}
				features.incrementCount(str, 1.0);
			}
			// Capitalization
			if (instance.length() > 0) {
				if (Character.isUpperCase(instance.charAt(0))) {
					features.incrementCount("CAP", 1.0);
				} else if (Character.isLowerCase(instance.charAt(0))) {
					features.incrementCount("UNCAP", 1.0);
				}
			}
			// Digits and other
			features.incrementCount(extractStyle(characters), 1.0);

			// -
			features.incrementCount(boundedString.contains('-') ? "Hyphen"
					: "NonHyphen", 1.0);
			return features;
		}
	}

	public static String extractStyle(char[] characters) {
		StringBuilder styleSB = new StringBuilder();
		char lastStyle = 'S', cur;
		for (int i = 0; i < characters.length; i++) {
			cur = characters[i];
			if (Character.isLetter(cur)) {
				if (Character.isUpperCase(cur)) {
					appendStyle(styleSB, 'X', lastStyle);
					lastStyle = 'X';
				} else {
					appendStyle(styleSB, 'x', lastStyle);
					lastStyle = 'x';
				}
			} else if (Character.isDigit(cur)) {
				appendStyle(styleSB, 'd', lastStyle);
				lastStyle = 'd';
			} else {
				appendStyle(styleSB, cur, lastStyle);
				lastStyle = cur;
			}
		}
		return styleSB.toString();
	}

	private static void appendStyle(StringBuilder SB, char c, char lastchar) {
		if (c == lastchar) {
			if (lastchar != SB.charAt(SB.length() - 1)) {
				return; // has repeated
			}
			SB.append('+');
			return;
		}
		SB.append(c);
	}

	public static final double MAXCOUNTTHRESHOLD = 5.0;

	public static List<LabeledInstance<String, String>> buildTrainingData(
			CounterMap<String, String> wordtotags) {
		List<LabeledInstance<String, String>> labeledInstances = new ArrayList<LabeledInstance<String, String>>();
		for (String word : wordtotags.keySet()) {
			Counter<String> tagCounter = wordtotags.getCounter(word);
			if (tagCounter.totalCount() > MAXCOUNTTHRESHOLD) {
				continue;
			}
			for (String tag : tagCounter.keySet()) {
				LabeledInstance<String, String> labeledInstance = new LabeledInstance<String, String>(
						tag, word);
				labeledInstances.add(labeledInstance);
			}
		}
		return labeledInstances;
	}

	public static <K, V> double GetPUNK(CounterMap<K, V> counterMap) {
		Counter<Double> frequency = new Counter<Double>();
		for (K key : counterMap.keySet()) {
			frequency.incrementCount(counterMap.getCounter(key).totalCount(),
					1.0);
		}
		return frequency.getCount(1.0);
	}
}
