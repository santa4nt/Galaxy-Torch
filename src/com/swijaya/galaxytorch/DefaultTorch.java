/**
 * Copyright (c) 2012 Santoso Wijaya
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.swijaya.galaxytorch;

import android.hardware.Camera;
import android.util.Log;

public class DefaultTorch implements CameraDevice.Torch {

    private static final String TAG = DefaultTorch.class.getSimpleName();

    /**
     * Generally, the following simple steps should suffice to turn on the
     * flashlight's torch mode.
     */
    public boolean toggleTorch(Camera camera, boolean on) {
        setFlashMode(camera, (on ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF));
        return true;
    }

    private void setFlashMode(Camera camera, String mode) {
        Log.v(TAG, "Setting flash mode: " + mode);
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(mode);
        camera.setParameters(params);
    }

}
