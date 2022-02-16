package org.telegram.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.vector.update_app.view.NumberProgressBar;

import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.RLottieImageView;

public class DownloadIngAlrt {
    private static AlertDialog.Builder downloadIngAlrt;
    private static NumberProgressBar progressBar;

    @SuppressLint("WrongConstant")
    public static void showDownloadIngAlrt(Activity context, String title, RLottieImageView topImageView) {
        LinearLayout linearLayout = new LinearLayout(context);
        progressBar = new NumberProgressBar(context,null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setReachedBarHeight(15f);
        progressBar.setUnreachedBarHeight(15f);
        progressBar.setProgressTextSize(30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        params.leftMargin = 40;
        params.rightMargin = 40;
        params.topMargin = 15;
        params.bottomMargin = 15;
        params.height = 50;
        linearLayout.addView(progressBar, params);
        downloadIngAlrt = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(linearLayout)
                .setTopView(topImageView);
        downloadIngAlrt.create().show();
    }

    public static void cancel() {
        if (downloadIngAlrt != null) {
            downloadIngAlrt.getDismissRunnable().run();
            downloadIngAlrt = null;
        }
    }


    public static void setProgress(int current) {
        if (downloadIngAlrt == null) {
            return;
        }
        progressBar.setProgress(current);
        if (progressBar.getProgress() >= progressBar.getMax()) {
            downloadIngAlrt.getDismissRunnable().run();
            downloadIngAlrt = null;
        }
    }

    public static void setProgress(long current) {
        if (downloadIngAlrt == null) {
            return;
        }
        progressBar.setProgress(((int) current) / (1024 * 1024));
        if (progressBar.getProgress() >= progressBar.getMax()) {
            downloadIngAlrt.getDismissRunnable().run();
            downloadIngAlrt = null;
        }
    }
}
