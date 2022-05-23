package com.vuquochung.foodapp.Callback;

import com.vuquochung.foodapp.Model.Order;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<Order> orderList);
    void onLoadOrderFailed(String message);
}
