package com.jeffthefate.dmbquiz.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.analytics.HitBuilders;
import com.jeffthefate.dmbquiz.ApplicationEx.ResourcesSingleton;
import com.jeffthefate.dmbquiz.Constants;
import com.jeffthefate.dmbquiz.ImageViewEx;
import com.jeffthefate.dmbquiz.R;

public class FragmentFaq extends FragmentBase {

    private Button doneButton;
    
    public FragmentFaq() {}
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if (mCallback != null) {
    		mCallback.setHomeAsUp(false);
    		mCallback.setInSetlist(false);
    	}
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        tracker.setScreenName("ActivityMain/FragmentFaq");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.faq, container, false);
        background = (ImageViewEx) v.findViewById(R.id.Background);
		setBackgroundBitmap(mCallback.getBackground(), "info");
        doneButton = (Button) v.findViewById(R.id.DoneButton);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory(Constants.CATEGORY_FRAGMENT_UI)
                        .setAction(Constants.ACTION_BUTTON_PRESS)
                        .setLabel("faqDone")
                        .setValue(1L)
                        .build());
                getActivity().onBackPressed();
            } 
        });
        return v;
    }
    
    @Override
    public void setBackground(Bitmap newBackground) {
    	if (background != null && newBackground != null) {
        	background.setImageDrawable(null);
        	BitmapDrawable bitmapDrawable = new BitmapDrawable(
        			ResourcesSingleton.instance(), newBackground);
        	bitmapDrawable.setColorFilter(new PorterDuffColorFilter(
        			ResourcesSingleton.instance().getColor(
        					R.color.background_dark),
					PorterDuff.Mode.SRC_ATOP));
        	background.setImageDrawable(bitmapDrawable);
        }
    }
    
}
