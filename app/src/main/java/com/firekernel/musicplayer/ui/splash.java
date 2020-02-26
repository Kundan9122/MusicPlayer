package com.firekernel.musicplayer.ui;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.firekernel.musicplayer.R;

public class splash extends AppCompatActivity {
    Animation anim;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_splash);
//        new Handler().postDelayed( new Runnable() {
//
//            @Override
//            public void run() {
//                // This method will be executed once the timer is over
//                Intent i = new Intent(splash.this, MainActivity.class);
//                startActivity(i);
//                finish();
//            }
//        }, 3000);
//    }
//}
        imageView=(ImageView)findViewById(R.id.imd); // Declare an imageView to show the animation.
        anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translation); // Create the animation.
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(splash.this,MainActivity.class));
                // HomeActivity.class is the activity to go after showing the splash screen.
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        imageView.startAnimation(anim);
    }
}