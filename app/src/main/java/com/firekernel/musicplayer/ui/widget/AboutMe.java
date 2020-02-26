package com.firekernel.musicplayer.ui.widget;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.ui.MainActivity;
import com.firekernel.musicplayer.utils.ActionHelper;

public class AboutMe extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_me);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.about_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.share_about:

                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent = new Intent( AboutMe.this, MainActivity.class );
        startActivity( intent );
        finish();
    }
}
