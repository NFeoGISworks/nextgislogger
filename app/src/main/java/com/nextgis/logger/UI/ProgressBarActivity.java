/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Authors: Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */

package com.nextgis.logger.UI;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.nextgis.logger.LoggerService;
import com.nextgis.logger.R;

public class ProgressBarActivity extends Activity implements View.OnClickListener {
    protected FloatingActionButton mFAB;
    protected int mThemeColor;
    protected boolean mHasFAB = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        setActionBarProgress(isLoggerServiceRunning(this));

        TypedArray array = getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorLink});
        mThemeColor = array.getColor(0, getResources().getColor(R.color.holo_blue));
        array.recycle();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        if (!mHasFAB)
            return;

        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        if (view instanceof FrameLayout) {
            FrameLayout base = (FrameLayout) view;

            if (base.findViewById(R.id.fab) == null) {
                FrameLayout layout = (FrameLayout) getLayoutInflater().inflate(R.layout.fab, base);
                mFAB = (FloatingActionButton) layout.findViewById(R.id.fab);
                layout.removeView(mFAB);
                base.addView(mFAB);

                mFAB.setColorNormal(darkerColor(mThemeColor, 0.7f));
                mFAB.setColorRipple(darkerColor(mThemeColor, 0.5f));
                mFAB.setColorPressed(darkerColor(mThemeColor, 0.3f));
                mFAB.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Toast.makeText(this, R.string.sessions_nothing_selected, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void setActionBarProgress(boolean state) {
        ActionBar actionBar = getActionBar();

        // retrieve the top view of our application
        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        ProgressBar progressBar = null;
        int osVersion = Build.VERSION.SDK_INT;

        for (int i = 0; i < decorView.getChildCount(); i++)
            if (decorView.getChildAt(i) instanceof ProgressBar) {
                progressBar = ((ProgressBar) decorView.getChildAt(i));
                break;
            }

        // create new ProgressBar and style it
        if (progressBar == null) {
            if (actionBar != null && osVersion < Build.VERSION_CODES.LOLLIPOP)
                actionBar.setBackgroundDrawable(null);

            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 24));
            progressBar.setProgress(100);
            decorView.addView(progressBar);

            // Here we try to position the ProgressBar to the correct position by looking
            // at the position where content area starts. But during creating time, sizes
            // of the components are not set yet, so we have to wait until the components
            // has been laid out
            // Also note that doing progressBar.setY(136) will not work, because of different
            // screen densities and different sizes of actionBar
            ViewTreeObserver observer = progressBar.getViewTreeObserver();
            final ProgressBar finalProgressBar = progressBar;
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View contentView = decorView.findViewById(android.R.id.content);
                    finalProgressBar.setY(contentView.getY() - 10);

                    ViewTreeObserver observer = finalProgressBar.getViewTreeObserver();
                    observer.removeGlobalOnLayoutListener(this);
                }
            });
        }

        if (state) {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setIndeterminate(false);

            if (osVersion >= Build.VERSION_CODES.LOLLIPOP)
                progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public static boolean isLoggerServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            if (LoggerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private int darkerColor(int color, float percent) {
        int r = Color.red(color);
        int b = Color.blue(color);
        int g = Color.green(color);

        return Color.rgb((int) (r * percent), (int) (g * percent), (int) (b * percent));
    }
}
