package net.sf.briar.api.serial;

import java.io.IOException;

public interface ObjectReader<T> {

	T readObject(Reader r) throws IOException;
}
