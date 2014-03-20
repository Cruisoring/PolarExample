package com.example.polarexample;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alouder.polarlayout.PolarLayout;
import com.alouder.polarlayout.PolarLayout.LayoutParams;

/*
 * Copyright (C) 2014 William JIANG
 * Created on Feb 25, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MainActivity extends Activity {
	static final String TAG="MainActivity";
	
    class azimuthAnimatorListenerAdapter extends AnimatorListenerAdapter {
    	
    	private LayoutParams params;
    	
    	public azimuthAnimatorListenerAdapter(View v) {
    		this.params = (LayoutParams) v.getLayoutParams();
    	}

		@Override
		public void onAnimationEnd(Animator animation) {
			if (params != null){
				float azimuth = params.getAzimuth();
				if (azimuth >= 360 || azimuth < 0) {
					
					params.setAzimuth(azimuth % 360);
				}
			}
		}
    	
    }
	
    private azimuthAnimatorListenerAdapter secondsAdapter, minutesAdapter, hoursAdapter;
	private Timer updateTimer;
	
	//Time needs for a Second/Minute hand move 1 unit or 6 degrees
	private final int MOVING_PERIOD = 500;
	private final int DEGREE_OFFSET = -90;
	private final float mSecondsPerHour = 1000f * 60 * 60;
	private final String ONE_SECOND_TIMER = "One Second Timer";
	
	PolarLayout dial;
	ImageView hoursView, minutesView, secondsView;
	TextView localDesc, localDateTime, localZoneIndicator;
	
	Time localTime  = new Time();
	Time utcTime = new Time(Time.TIMEZONE_UTC);
	TimeZone currentTimeZone = null;
	SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, E, HH:mm");
	List<TimeZone> timeZones = new ArrayList<TimeZone>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		
		init();
		
		localTime.setToNow();
		long ticks = localTime.toMillis(true);
		long mSec = ticks % 1000;
		updateTimer = new Timer(ONE_SECOND_TIMER);
		
		//Schedule the tick every 1 second
		updateTimer.schedule(tickTask, 1000 - mSec, 1000);
	}

	private void init() {
		dial = (PolarLayout)this.findViewById(R.id.dial);
		hoursView = (ImageView)this.findViewById(R.id.hour);
		minutesView = (ImageView)this.findViewById(R.id.minute);
		secondsView = (ImageView)this.findViewById(R.id.second);
		localDesc = (TextView)this.findViewById(R.id.localDesc);
		localDateTime = (TextView)this.findViewById(R.id.localTime);
		
		secondsAdapter = new azimuthAnimatorListenerAdapter(secondsView);
		minutesAdapter = new azimuthAnimatorListenerAdapter(minutesView);
		hoursAdapter = new azimuthAnimatorListenerAdapter(hoursView);
		
		PolarLayout.LayoutParams params = null;
		
		//*/ //Demo of adding child views in codes
		String tickMark=".";
		for(int i=1; i<60; i++){
			if (i%5==0)
				continue;
			TextView t = new TextView(this);
			t.setText(tickMark);
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.radius = 0.9f;
			int d = i*6;
			params.setFixed(true);
			params.setAzimuth(d);
			params.keepOrientation = true;
//			params.setOrientation(d);
			dial.addView(t, params);
		}
		//*/ //End of Demo of adding child views in codes
		
		//Initialize the hands to show the UTC time
		localTime.setToNow();
		utcTime.setToNow();
		
		float hoursDegree = getHoursDegrees(localTime.hour, localTime.minute);
		adjustAzimuth(hoursView, hoursDegree);
		adjustAzimuth(minutesView, getMinSecDegrees(localTime.minute));
		adjustAzimuth(secondsView, getMinSecDegrees(localTime.second));
		
		currentTimeZone = TimeZone.getDefault();
		timeZones.add(currentTimeZone);
		String timeZoneDesc = currentTimeZone.getDisplayName(true, TimeZone.LONG);
		localDesc.setText(timeZoneDesc);
		updateTime();
		
		localZoneIndicator = new TextView(this);
		localZoneIndicator.setText(timeZoneDesc);
		localZoneIndicator.setBackgroundColor(0x33445500);
		params = new LayoutParams(-2, -2);
		params.radius = 0.45f;
		params.setAzimuth(hoursDegree);
		params.setOrientation(0);
		params.setFixed(false);
		params.keepOrientation = true;
		dial.addView(localZoneIndicator, params);
		
//		//Get the time difference in hours with UTC time
//		float hourDifference = currentTimeZone.getRawOffset()/mSecondsPerHour;
//		
//		//Get the degrees shall be adjusted
//		float degrees = getHoursDegrees(hourDifference);
//		
//		//Adjust the spinning of the dial to show utcTime as the default local time
//		dial.setSpinning(-degrees);
	}

	private void adjustAzimuth(View v, float azimuth) {
		PolarLayout.LayoutParams params = (LayoutParams) v.getLayoutParams();
		if (params != null) {			
			params.setAzimuth(azimuth);
			
			if (!params.keepOrientation){
				params.setOrientation(azimuth);
			}
		}
	}
	
	private TimerTask tickTask = new TimerTask() {
		
		@Override
		public void run() {
			Message message = new Message();
			message.what=101003;
			mHandler.sendMessage(message);
		}
	};
	
	private void updateTime(){
		Date date = new Date(localTime.toMillis(true));
		localDateTime.setText(dateFormat.format(date));
	}
	
	private Handler mHandler = new Handler(){
	    @Override
		public void handleMessage(Message msg) {
	    	localTime.setToNow();
			
			List<Animator> animators = new ArrayList<Animator>();
			
			//The PolarLayout must be involved to make others moving
//			ObjectAnimator dialAnimator = ObjectAnimator.ofInt(dial, "nothing", 0);
			ObjectAnimator dialAnimator = ObjectAnimator.ofFloat(dial, "spinning", dial.getSpinning());
			dialAnimator.setDuration(MOVING_PERIOD+100);
			animators.add(dialAnimator);
			
			int s, m, h;
			s = localTime.second;
			float destDegrees = getMinSecDegrees(s);
			ObjectAnimator animator = getAzimuthAnimator(secondsView, destDegrees, secondsAdapter);
			if (animator != null)
				animators.add(animator);
			
			//Change minute and hour hand only when necessary
			if (s == 0) {
				m = localTime.minute;
				h = localTime.hour;
				destDegrees = getMinSecDegrees(m+1);
				animator = getAzimuthAnimator(minutesView, destDegrees, minutesAdapter);
				if (animator != null)
					animators.add(animator);
				
				destDegrees = getHoursDegrees(h, m);
				animator = getAzimuthAnimator(hoursView, destDegrees, hoursAdapter);
				if (animator != null)
					animators.add(animator);
				
				//Update the localZoneIndicator as well
				animator = getAzimuthAnimator(localZoneIndicator, destDegrees, hoursAdapter);
				if (animator != null)
					animators.add(animator);
				
				TextView t = (TextView)dial.findViewById(1000);
				if (t != null){
					animator = getAzimuthAnimator(t, destDegrees, hoursAdapter);
					if (animator != null)
						animators.add(animator);					
				}
				
				updateTime();
			}
			
			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.playTogether(animators);
			animatorSet.start();
	    }
	    
		private ObjectAnimator getAzimuthAnimator(View child, float destDegrees, azimuthAnimatorListenerAdapter adapter) {
			LayoutParams params = (LayoutParams) child.getLayoutParams();
			if (params == null)
				return null;
			
			float azimuth = params.getAzimuth();
			if (azimuth > destDegrees) {
				destDegrees += 360;
			}
			ObjectAnimator result = ObjectAnimator.ofFloat(params, 
					"azimuth", destDegrees);
			result.setDuration(MOVING_PERIOD);
			result.addListener(adapter);
			
			return result;
		}
		
	};
	
	private float getMinSecDegrees(int minSec) {
		float result = (minSec % 60) * 6 + DEGREE_OFFSET;
		return result;
	}
	
	private float getHoursDegrees(float hoursDif) {
		return hoursDif * 30 + DEGREE_OFFSET;
	}
	
	private float getHoursDegrees(int hours, int minutes) {
		float portion = (minutes%60)/60.0f;
		return getHoursDegrees(hours % 12 + portion);
	}
}
