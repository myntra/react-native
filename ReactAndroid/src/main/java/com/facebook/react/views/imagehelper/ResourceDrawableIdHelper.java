/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * <p>This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 */
package com.facebook.react.views.imagehelper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import android.util.Log;
import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.*;
import android.content.ContextWrapper;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.ThemedReactContext;
import android.os.Looper;
import android.os.Handler;

/** Helper class for obtaining information about local images. */
@ThreadSafe
public class ResourceDrawableIdHelper {

  private Map<String, Integer> mResourceDrawableIdMap;

  private static final String LOCAL_RESOURCE_SCHEME = "res";
  private static volatile ResourceDrawableIdHelper sResourceDrawableIdHelper;
  private SplitInstallManager manager;

  private ResourceDrawableIdHelper() {
    mResourceDrawableIdMap = new HashMap<String, Integer>();
  }

  public static ResourceDrawableIdHelper getInstance() {
    if (sResourceDrawableIdHelper == null) {
      synchronized (ResourceDrawableIdHelper.class) {
        if (sResourceDrawableIdHelper == null) {
          sResourceDrawableIdHelper = new ResourceDrawableIdHelper();
        }
      }
    }
    return sResourceDrawableIdHelper;
  }

  public synchronized SplitInstallManager getSplitInstallManager(Context context) {
    this.manager = SplitInstallManagerFactory.create(context);

    Log.d("DFM", "Hello DFM ResDrHelper = " + this.manager);
    return this.manager;
  }

  public synchronized void clear() {
    mResourceDrawableIdMap.clear();
  }

  public int getResourceDrawableId(Context context, @Nullable String name) {
    if (name == null || name.isEmpty()) {
      return 0;
    }
    name = name.toLowerCase().replace("-", "_");

    // name could be a resource id.
    try {
      return Integer.parseInt(name);
    } catch (NumberFormatException e) {
      // Do nothing.
    }

    synchronized (this) {
      if (mResourceDrawableIdMap.containsKey(name)) {
        return mResourceDrawableIdMap.get(name);
      }
      final int[] id = {context.getResources().getIdentifier(name, "drawable", context.getPackageName())};
      String finalName = name;
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          try {
            if (id[0] == 0) {
              SplitInstallHelper.updateAppInfo(((ThemedReactContext) context).getCurrentActivity());
              SplitCompat.install(((ThemedReactContext) context).getCurrentActivity());
              new Handler().post(new Runnable() {
                @Override
                public void run(){
                  java.util.Set<java.lang.String> modules = getSplitInstallManager(((ThemedReactContext) context).getCurrentActivity()).getInstalledModules();
                  for (String moduleName: modules) {
                    String packageName = context.getApplicationContext().getPackageName() + "." + moduleName;
                    id[0] = context.getApplicationContext().getResources().getIdentifier(finalName, "drawable", packageName);
                    if (id[0] > 0) break;
                  }
                }
              });
            }

          } catch (Exception e) {

          }

        }
      });


//        String packageName = context.getApplicationContext().getPackageName() + "." + "move";
//        id = context.getApplicationContext().getResources().getIdentifier(name, "drawable", packageName);
        Log.d("DFM", "Hello DFM ResDrHelper id = " + id[0]);
        // Drawable drawable = context.getResources().getDrawable(id);
      if(id[0] > 0) mResourceDrawableIdMap.put(name, id[0]);
      return id[0];
      }

    }


  public @Nullable Drawable getResourceDrawable(Context context, @Nullable String name) {
    int resId = getResourceDrawableId(context, name);
    return resId > 0 ? context.getResources().getDrawable(resId) : null;
  }

  public Uri getResourceDrawableUri(Context context, @Nullable String name) {
    int resId = getResourceDrawableId(context, name);
    return resId > 0
        ? new Uri.Builder().scheme(LOCAL_RESOURCE_SCHEME).path(String.valueOf(resId)).build()
        : Uri.EMPTY;
  }
}
