package org.bpel4chor.splitprocess.pwdg.model;

import org.eclipse.bpel.model.Activity;

/**
 * WDG Node contains an activity
 * 
 * @since Mar 1, 2012
 * @author Daojun Cui
 */
public class WDGNode {

	protected Activity activity;

	public WDGNode(Activity activity) {
		if (activity == null)
			throw new NullPointerException();
		this.activity = activity;
	}

	public String getName() {
		return this.activity.getName();
	}
	
	public Activity getActivity() {
		return this.activity;
	}
	
	public Activity activity() {
		return getActivity();
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public boolean equals(WDGNode theOther) {
		return theOther == null ? this.activity == null : this.activity.equals(theOther.activity());
	}

	public String toString() {
		return this.activity.getName();
	}
	
}
