package org.opencv.samples.facedetect;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HistoricalInfoFragment extends Fragment {
	TextView finalHistoryTextView;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater .inflate(R.layout.historyfragment, container, false);
		
		return view;
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        finalHistoryTextView = (TextView) findB
    }
}
