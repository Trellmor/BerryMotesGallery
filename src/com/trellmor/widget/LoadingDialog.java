package com.trellmor.widget;

import com.trellmor.berrymotes.gallery.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.widget.ImageView;
import android.widget.TextView;

public class LoadingDialog extends Dialog {
	private TextView mTextMessage;
	private ImageView mImage;

	public LoadingDialog(Context context) {
		super(context, R.style.Theme_Dialog_NoActionBar);
		setContentView(R.layout.dialog_loading);

		mTextMessage = (TextView) findViewById(R.id.text_message);
		mImage = (ImageView) findViewById(R.id.image_loading);
		mImage.setBackgroundResource(R.drawable.loading);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		AnimationDrawable drawable = (AnimationDrawable) mImage.getBackground();
		drawable.start();
	}

	public void setText(int resid) {
		mTextMessage.setText(resid);
	}

	public void setText(CharSequence text) {
		mTextMessage.setText(text);
	}
}
