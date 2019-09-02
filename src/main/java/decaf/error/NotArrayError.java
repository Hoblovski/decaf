package decaf.error;

import decaf.tree.Pos;

/**
 * example：[] can only be applied to arrays<br>
 * PA2
 */
public class NotArrayError extends DecafError {

	public NotArrayError(Pos pos) {
		super(pos);
	}

	@Override
	protected String getErrMsg() {
		return "[] can only be applied to arrays";
	}

}
