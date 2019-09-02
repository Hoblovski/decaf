package decaf.error;

import decaf.tree.Pos;

/**
 * example：new array length must be an integer<br>
 * PA2
 */
public class BadNewArrayLength extends DecafError {

	public BadNewArrayLength(Pos pos) {
		super(pos);
	}

	@Override
	protected String getErrMsg() {
		return "new array length must be an integer";
	}

}
