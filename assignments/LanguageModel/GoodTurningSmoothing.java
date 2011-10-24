package nlp.assignments.LanguageModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class GoodTurningSmoothing {
	private static final double k = 6.0;

	/**
	 * Smooth for Unigram by give UNKNOWN data a reasonable count and adjust
	 * others'
	 * 
	 * @param <K>
	 * @param counter
	 * @return the count for UNKNOWN data
	 */
	static <K> double Smooth(Counter<K> counter) {
		Counter<Double> frequency = new Counter<Double>();
		for (K key : counter.keySet()) {
			frequency.incrementCount(counter.getCount(key), 1.0);
		}

		Map<Double, Double> rMap = GetFrequencyMap(frequency);

		for (K key : counter.keySet()) {
			double r = counter.getCount(key); // old value
			if (rMap.containsKey(r)) {
				counter.setCount(key, rMap.get(r));
			}
		}

		return frequency.getCount(1.0);
	}

	static Map<Double, Double> GetFrequencyMap(Counter<Double> frequency) {
		Map<Double, Double> rMap = new HashMap<Double, Double>();

		for (double r = 1.0; r < k; r++) {
			double rstar = (r + 1.0) * frequency.getCount(r + 1)
					/ frequency.getCount(r);
			double mu = k * frequency.getCount(k) / frequency.getCount(1.0);
			double d = (rstar / r - mu) / (1 - mu);
			rMap.put(r, d * r);
			// System.out.println(String.format("c: %f\tc*: %f\tnumber: %f", r,
			// rMap.get(r), frequency.getCount(r)));
		}
		return rMap;
	}

	/**
	 * Smooth for Bigram by give UNKNOWN data a reasonable count and adjust
	 * others'
	 * 
	 * @param <K>
	 * @param counter
	 * @return the count for UNKNOWN data
	 */
	static <K, V> double Smooth(CounterMap<K, V> counterMap) {
		Counter<Double> frequency = new Counter<Double>();
		for (K key : counterMap.keySet()) {
			Counter<V> vCounter = counterMap.getCounter(key);
			for (V v : vCounter.keySet())
				frequency.incrementCount(vCounter.getCount(v), 1.0);
		}

		Map<Double, Double> rMap = GetFrequencyMap(frequency);

		for (K key : counterMap.keySet()) {
			Counter<V> vCounter = counterMap.getCounter(key);
			for (V v : vCounter.keySet()) {
				double r = vCounter.getCount(v); // old value
				if (rMap.containsKey(r)) {
					counterMap.setCount(key, v, rMap.get(r));
				}
			}
		}
		return frequency.getCount(1.0);
	}

	/**
	 * Smooth for Bigram by give UNKNOWN data a reasonable count and adjust
	 * others'. Add UNKKey to each counter in the counterMap, normalized
	 * 
	 * @param <K>
	 * @param counter
	 * @return the count for UNKNOWN data
	 */
	public static <K, V> double Smooth(CounterMap<K, V> counterMap, V UNKKey) {
		Counter<Double> frequency = new Counter<Double>();
		for (K key : counterMap.keySet()) {
			Counter<V> vCounter = counterMap.getCounter(key);
			for (V v : vCounter.keySet())
				frequency.incrementCount(vCounter.getCount(v), 1.0);
		}

		Map<Double, Double> rMap = GetFrequencyMap(frequency);

		for (K key : counterMap.keySet()) {
			Counter<V> vCounter = counterMap.getCounter(key);
			double unkcount = 0;
			for (V v : vCounter.keySet()) {
				double r = vCounter.getCount(v); // old value
				if (rMap.containsKey(r)) {
					unkcount += r - rMap.get(r);
				}
			}
			if (unkcount == 0) {
				unkcount = 1E-4; // avoid zero for unknown word
			}
			counterMap.incrementCount(key, UNKKey, unkcount);
		}
		counterMap.normalize();
		return frequency.getCount(1.0);
	}
}
