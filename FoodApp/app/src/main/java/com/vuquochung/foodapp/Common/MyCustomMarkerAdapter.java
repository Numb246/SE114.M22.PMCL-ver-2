package com.vuquochung.foodapp.Common;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.vuquochung.foodapp.R;

public class MyCustomMarkerAdapter implements GoogleMap.InfoWindowAdapter{

    private View itemView;

    public MyCustomMarkerAdapter(LayoutInflater inflater)
    {
        itemView=inflater.inflate(R.layout.layout_marker_display,null);
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        TextView txt_shipper_name=(TextView) itemView.findViewById(R.id.txt_shipper_name);
        TextView txt_shipper_info=(TextView) itemView.findViewById(R.id.txt_shipper_info);

        txt_shipper_name.setText(marker.getTitle());
        txt_shipper_info.setText(marker.getSnippet());
        return itemView;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }
}
