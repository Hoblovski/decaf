package decaf.error;

import decaf.tree.Pos;

/**
 * example：unterminated string constant: "this is str"<br>
 * PA1
 */
public class UntermStrError extends DecafError {

	private String str;

	public UntermStrError(Pos pos, String str) {
		super(pos);
		this.str = str;
	}

	@Override
	protected String getErrMsg() {
		return "unterminated string constant " + str;
	}

}
