package com.vuquochung.foodapp.Callback;

import com.vuquochung.foodapp.Model.CategoryModel;
import com.vuquochung.foodapp.Model.RestaurantModel;

import java.util.List;

public interface IRestaurantCallbackListener {
    void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList);
    void onRestaurantLoadFailed(String message);
}
