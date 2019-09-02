package decaf.error;

import decaf.tree.Pos;

/**
 * example：integer literal 112233445566778899 is too large<br>
 * PA1
 */
public class IntTooLargeError extends DecafError {

	private String val;

	public IntTooLargeError(Pos pos, String val) {
		super(pos);
		this.val = val;
	}

	@Override
	protected String getErrMsg() {
		return "integer literal " + val + " is too large";
	}

}
