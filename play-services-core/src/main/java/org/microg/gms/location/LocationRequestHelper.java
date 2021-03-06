/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationRequestHelper {
    public static final String TAG = "GmsLocRequestHelper";
    private final Context context;
    public final LocationRequest locationRequest;
    public final boolean hasFinePermission;
    public final boolean hasCoarsePermission;
    public final String packageName;
    private ILocationListener listener;
    private PendingIntent pendingIntent;

    private Location lastReport;
    private int numReports = 0;

    public LocationRequestHelper(Context context, LocationRequest locationRequest,
            boolean hasFinePermission, boolean hasCoarsePermission, String packageName,
            ILocationListener listener) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.hasFinePermission = hasFinePermission;
        this.hasCoarsePermission = hasCoarsePermission;
        this.packageName = packageName;
        this.listener = listener;
    }

    public LocationRequestHelper(Context context, LocationRequest locationRequest,
            boolean hasFinePermission, boolean hasCoarsePermission, String packageName,
            PendingIntent pendingIntent) {
        this.context = context;
        this.locationRequest = locationRequest;
        this.hasFinePermission = hasFinePermission;
        this.hasCoarsePermission = hasCoarsePermission;
        this.packageName = packageName;
        this.pendingIntent = pendingIntent;
    }

    /**
     * @return whether to continue sending reports to this {@link LocationRequestHelper}
     */
    public boolean report(Location location) {
        if (lastReport != null) {
            if (location.getTime() - lastReport.getTime() < locationRequest.getFastestInterval()) {
                return true;
            }
            if (location.distanceTo(lastReport) < locationRequest.getSmallestDesplacement()) {
                return true;
            }
        }
        Log.d(TAG, "sending Location: " + location);
        if (listener != null) {
            try {
                listener.onLocationChanged(location);
            } catch (RemoteException e) {
                return false;
            }
        } else if (pendingIntent != null) {
            Intent intent = new Intent();
            intent.putExtra("com.google.android.location.LOCATION", location);
            try {
                pendingIntent.send(context, 0, intent);
            } catch (PendingIntent.CanceledException e) {
                return false;
            }
        }
        lastReport = location;
        numReports++;
        return numReports < locationRequest.getNumUpdates();
    }

    @Override
    public String toString() {
        return "LocationRequestHelper{" +
                "locationRequest=" + locationRequest +
                ", hasFinePermission=" + hasFinePermission +
                ", hasCoarsePermission=" + hasCoarsePermission +
                ", packageName='" + packageName + '\'' +
                ", lastReport=" + lastReport +
                '}';
    }

    public boolean respondsTo(ILocationListener listener) {
        return this.listener != null && listener != null &&
                this.listener.asBinder().equals(listener.asBinder());
    }

    public boolean respondsTo(PendingIntent pendingIntent) {
        return this.pendingIntent != null && this.pendingIntent.equals(pendingIntent);
    }
}
