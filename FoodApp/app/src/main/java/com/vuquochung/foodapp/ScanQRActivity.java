package com.vuquochung.foodapp;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.vuquochung.foodapp.Common.Common;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanQRActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{
    
    @BindView(R.id.zxscan)
    ZXingScannerView zXingScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        
        initViews();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        zXingScannerView.setResultHandler(ScanQRActivity.this::handleResult);
                        zXingScannerView.startCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(ScanQRActivity.this,"Please enable CAMERA permission",Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    private void initViews() {
        ButterKnife.bind(this);
    }

    @Override
    protected void onStop() {
        zXingScannerView.stopCamera();
        super.onStop();
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent intent=new Intent();
        intent.putExtra(Common.QR_CODE_TAG,rawResult.getText());
        setResult(Activity.RESULT_OK,intent);
        finish();
    }
}