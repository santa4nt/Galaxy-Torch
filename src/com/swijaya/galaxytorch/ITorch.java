package com.swijaya.galaxytorch;

import android.hardware.Camera;

public interface ITorch {
	
	public boolean toggleTorch(Camera camera, boolean on);

}
