package decaf.error;

import decaf.tree.Pos;

/**
 * example：function 'length' expects 0 argument(s) but 2 given<br>
 * PA2
 */
public class BadLengthArgError extends DecafError {

	private int count;

	public BadLengthArgError(Pos pos, int count) {
		super(pos);
		this.count = count;
	}

	@Override
	protected String getErrMsg() {
		return "function 'length' expects 0 argument(s) but " + count
				+ " given";
	}

}
