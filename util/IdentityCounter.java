package nlp.util;

/**
 * Convenience Extension of Counter to use an IdentityHashMap.
 */
public class IdentityCounter<E> extends Counter<E> {
  public IdentityCounter() {
    super(new MapFactory.IdentityHashMapFactory<E,Double>());
  }
}
