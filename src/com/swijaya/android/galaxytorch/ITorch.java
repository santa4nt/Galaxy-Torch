package com.swijaya.android.galaxytorch;

import android.hardware.Camera;

public interface ITorch {
	
	public boolean turnOnTorch(Camera camera);
	
	public boolean turnOffTorch(Camera camera);

}
