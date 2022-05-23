package com.vuquochung.foodapp.Callback;

import com.vuquochung.foodapp.Model.OrderModel;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<OrderModel> orderModelList);
    void onLoadOrderFailed(String message);
}
