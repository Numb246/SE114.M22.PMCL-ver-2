package com.vuquochung.foodapp.Callback;

import com.vuquochung.foodapp.Model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs);
    void onLoadOnlyTimeSuccess(long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
