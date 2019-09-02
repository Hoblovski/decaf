package decaf.error;

import decaf.tree.Pos;

/**
 * example：'length' can only be applied to arrays<br>
 * PA2
 */
public class BadLengthError extends DecafError {

	public BadLengthError(Pos pos) {
		super(pos);
	}

	@Override
	protected String getErrMsg() {
		return "'length' can only be applied to arrays";
	}

}
