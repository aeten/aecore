package org.pititom.core.parsing;

import java.io.Reader;
import org.pititom.core.Identifiable;

/**
 *
 * @author Thomas Pérennou
 */
public interface MarkupConverter<T> extends Identifiable {
	public T convert(Reader reader, Parser<MarkupNode> parser);
}
