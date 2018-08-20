package com.gesturecaller;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Intro Created by Ashish on 12/21/2017.
 */

public class Intro extends AppCompatActivity {

    private static final String inactiveDot = "&#9675;";
    private static final String activeDot = "&#9679;";
    ArrayList<DataSet> dataSets = new ArrayList<>();
    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private Button btnSkip, btnNext;
    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            setBtnColors(position);

            // changing the next button text 'NEXT' / 'GOT IT'
            if (position == dataSets.size() - 1) {
                // last page. make button text to GOT IT
                String placeholder = "GOT IT";
                btnNext.setText(placeholder);
                btnSkip.setVisibility(View.GONE);
            } else {
                // still pages are left
                String placeholder = "NEXT";
                btnNext.setText(placeholder);
                btnSkip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    private void prepareData() {
        DataSet dataSet = new DataSet("file:///android_asset/gesture_screen.png", "Setup upto 8 easy-to-use, distraction free, single-finger gestures");
        dataSets.add(dataSet);

        dataSet = new DataSet("file:///android_asset/gesture_set.png", "... or use default gesture settings instead.");
        dataSets.add(dataSet);

        dataSet = new DataSet("file:///android_asset/bike_mode.png", "Enable bike mode and auto-reject all calls except the important ones.");
        dataSets.add(dataSet);

        dataSet = new DataSet("file:///android_asset/location.png", "Enable location mode and roam carefree. Long press a location to delete.");
        dataSets.add(dataSet);

        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnNext.setVisibility(View.VISIBLE);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checking for last page
                // if last page home screen will be launched
                int current = getItem(+1);
                if (current < dataSets.size()) {
                    // move to next screen
                    viewPager.setCurrentItem(current);
                } else {
                    launchHomeScreen();
                }
            }
        });

        if (dataSets.size() > 0) {
            setBtnColors(0);
        }
    }

    private void setBtnColors(int position) {
        btnNext.setTextColor(Color.parseColor(dataSets.get(position).cBtn));
        btnSkip.setTextColor(Color.parseColor(dataSets.get(position).cBtn));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchHomeScreen();
            }
        });
        prepareData();
    }

    private void addBottomDots(int currentPage) {
        TextView[] dots = new TextView[dataSets.size()];

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml(inactiveDot));
            dots[i].setTextSize(18);
            dots[i].setTextColor(Color.parseColor(dataSets.get(currentPage).cInactive));
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[currentPage].setTextColor(Color.parseColor(dataSets.get(currentPage).cActive));
            dots[currentPage].setText(Html.fromHtml(activeDot));
        }
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen() {
        //startActivity(new Intent(Intro.this, MainActivity.class));
        finish();
    }

    /**
     * Making notification bar transparent
     */

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#22999999"));
        }
    }

    @SuppressWarnings("WeakerAccess")
    static public class DataSet {
        public String info;
        public String path;
        public String cActive = "#000000";
        public String cInactive = "#777777";
        public String cBg = "#ffffff";
        public String cBtn = "#333333";
        public String cInfo = "#444444";

        public DataSet(String path, String info) {
            this.path = path;
            this.info = info;
        }
    }

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            assert layoutInflater != null;
            View view = layoutInflater.inflate(R.layout.model_intro_screens, container, false);

            final DataSet ds = dataSets.get(position);
            RelativeLayout rl_background = view.findViewById(R.id.rl_mis_container);
            rl_background.setBackgroundColor(Color.parseColor(ds.cBg));


            if (!ds.cInfo.equals("null")) {
                TextView tv_basic_info = view.findViewById(R.id.tv_mis_basic_info);
                tv_basic_info.setVisibility(View.VISIBLE);
                tv_basic_info.setTextColor(Color.parseColor(ds.cInfo));
                tv_basic_info.setText(ds.info);
            }
            ImageView iv_screen = view.findViewById(R.id.iv_mis_pic);
            Glide.with(Intro.this).load(Uri.parse(ds.path)).into(iv_screen);

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return dataSets.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}
